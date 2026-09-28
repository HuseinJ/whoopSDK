package com.whoopsdk.api;

import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.http.Json;
import com.whoopsdk.http.WhoopHttpClient;
import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;
import com.whoopsdk.model.Sleep;
import com.whoopsdk.pagination.AutoPaging;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Sleep activities — endpoints under {@code /v2/activity/sleep}. Reached via {@code whoop.sleeps()}.
 *
 * <p>All methods require the {@code read:sleep} scope.
 */
public final class SleepApi {

    private static final JavaType PAGE_TYPE =
            Json.mapper().getTypeFactory().constructParametricType(Page.class, Sleep.class);

    private final WhoopHttpClient http;

    public SleepApi(WhoopHttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    /** A single sleep by its v2 UUID. */
    public Sleep getSleep(UUID sleepId) {
        return http.get("/v2/activity/sleep/" + sleepId, Map.of(), Sleep.class);
    }

    /** A single sleep by its v2 UUID in string form. */
    public Sleep getSleep(String sleepId) {
        return getSleep(UUID.fromString(sleepId));
    }

    /** One page of sleeps, newest first. Includes naps. */
    public Page<Sleep> listSleeps(PageRequest request) {
        return http.get("/v2/activity/sleep", request.toQueryParams(), PAGE_TYPE);
    }

    /** The most recent sleeps with the API's default page size. */
    public Page<Sleep> listSleeps() {
        return listSleeps(PageRequest.unpaged());
    }

    /** Every sleep matching {@code request}, paging lazily as the stream is consumed. */
    public Stream<Sleep> streamSleeps(PageRequest request) {
        return AutoPaging.stream(this::listSleeps, request);
    }

    /** Every sleep in the user's history, newest first. */
    public Stream<Sleep> streamSleeps() {
        return streamSleeps(PageRequest.of(PageRequest.MAX_LIMIT));
    }

    /** Nights only — the same as {@link #streamSleeps(PageRequest)} with naps filtered out. */
    public Stream<Sleep> streamNights(PageRequest request) {
        return streamSleeps(request).filter(sleep -> !sleep.nap());
    }
}
