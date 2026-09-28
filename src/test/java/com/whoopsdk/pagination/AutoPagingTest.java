package com.whoopsdk.pagination;

import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoPagingTest {

    @Test
    void walksEveryPage() {
        List<Page<String>> pages = List.of(
                new Page<>(List.of("a", "b"), "t1"),
                new Page<>(List.of("c", "d"), "t2"),
                new Page<>(List.of("e"), null));

        List<String> seen = AutoPaging.stream(fetcherFor(pages, new ArrayList<>()), PageRequest.of(2))
                .collect(Collectors.toList());

        assertEquals(List.of("a", "b", "c", "d", "e"), seen);
    }

    @Test
    void passesTheNextTokenToTheFollowingRequest() {
        List<Page<String>> pages = List.of(
                new Page<>(List.of("a"), "t1"),
                new Page<>(List.of("b"), null));
        List<String> tokensSeen = new ArrayList<>();

        AutoPaging.stream(fetcherFor(pages, tokensSeen), PageRequest.of(1))
                .forEach(s -> {
                });

        assertEquals(2, tokensSeen.size());
        assertEquals("<none>", tokensSeen.get(0));
        assertEquals("t1", tokensSeen.get(1));
    }

    @Test
    void fetchesLazilyWhenTheStreamIsBounded() {
        AtomicInteger calls = new AtomicInteger();
        List<Page<String>> pages = List.of(
                new Page<>(List.of("a", "b"), "t1"),
                new Page<>(List.of("c", "d"), "t2"),
                new Page<>(List.of("e"), null));

        List<String> seen = AutoPaging.stream(request -> {
            calls.incrementAndGet();
            return pages.get(calls.get() - 1);
        }, PageRequest.of(2)).limit(3).collect(Collectors.toList());

        assertEquals(List.of("a", "b", "c"), seen);
        assertEquals(2, calls.get(), "should not have fetched the third page");
    }

    @Test
    void handlesAnEmptyFirstPage() {
        List<Page<String>> pages = List.of(new Page<>(List.of(), null));

        assertTrue(AutoPaging.stream(fetcherFor(pages, new ArrayList<>()), PageRequest.of(5))
                .findAny().isEmpty());
    }

    @Test
    void skipsAnEmptyPageInTheMiddle() {
        List<Page<String>> pages = List.of(
                new Page<>(List.of("a"), "t1"),
                new Page<>(List.of(), "t2"),
                new Page<>(List.of("b"), null));

        List<String> seen = AutoPaging.stream(fetcherFor(pages, new ArrayList<>()), PageRequest.of(1))
                .collect(Collectors.toList());

        assertEquals(List.of("a", "b"), seen);
    }

    private static java.util.function.Function<PageRequest, Page<String>> fetcherFor(
            List<Page<String>> pages, List<String> tokensSeen) {
        AtomicInteger index = new AtomicInteger();
        return request -> {
            String token = request.nextTokenValue();
            tokensSeen.add(token == null ? "<none>" : token);
            return pages.get(index.getAndIncrement());
        };
    }
}
