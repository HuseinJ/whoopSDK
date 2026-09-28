package com.whoopsdk.pagination;

import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Turns a page-at-a-time endpoint into a lazy {@link Stream}.
 *
 * <p>Pages are fetched on demand as the stream is consumed, so a bounded operation such as
 * {@code .limit(10)} only triggers the requests it actually needs — which matters given WHOOP's
 * 100 requests/minute budget.
 */
public final class AutoPaging {

    private AutoPaging() {
    }

    /**
     * Streams every record reachable from {@code request}, fetching pages as they are needed.
     *
     * @param <T>     the record type
     * @param fetch   fetches one page for a given request
     * @param request the initial request; its {@code nextToken} is replaced as paging advances
     * @return a lazy stream over all records, in the order the API returns them
     */
    public static <T> Stream<T> stream(Function<PageRequest, Page<T>> fetch, PageRequest request) {
        Iterator<T> iterator = iterator(fetch, request);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL),
                false);
    }

    /**
     * The same traversal as {@link #stream}, exposed as an {@link Iterator}.
     *
     * @param <T>     the record type
     * @param fetch   fetches one page for a given request
     * @param request the initial request; its {@code nextToken} is replaced as paging advances
     * @return an iterator that fetches the next page only when the current one runs out
     */
    public static <T> Iterator<T> iterator(Function<PageRequest, Page<T>> fetch, PageRequest request) {
        return new PagingIterator<>(fetch, request);
    }

    private static final class PagingIterator<T> implements Iterator<T> {

        private final Function<PageRequest, Page<T>> fetch;
        private PageRequest request;
        private List<T> current = List.of();
        private int index;
        private boolean exhausted;

        PagingIterator(Function<PageRequest, Page<T>> fetch, PageRequest request) {
            this.fetch = fetch;
            this.request = request;
        }

        @Override
        public boolean hasNext() {
            while (index >= current.size()) {
                if (exhausted) {
                    return false;
                }
                Page<T> page = fetch.apply(request);
                current = page.records();
                index = 0;
                if (page.hasNext()) {
                    request = request.withNextToken(page.nextToken());
                } else {
                    exhausted = true;
                }
                // An empty page mid-stream is possible; loop until we find records or run out.
                if (current.isEmpty() && exhausted) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current.get(index++);
        }
    }
}
