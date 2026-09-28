package com.whoopsdk.api;

import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.http.Json;
import com.whoopsdk.http.WhoopHttpClient;
import com.whoopsdk.model.Cycle;
import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;
import com.whoopsdk.model.Sleep;
import com.whoopsdk.pagination.AutoPaging;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Physiological cycles — endpoints under {@code /v2/cycle}. Reached via {@code whoop.cycles()}.
 *
 * <p>All methods require the {@code read:cycles} scope.
 */
public final class CycleApi {

    private static final JavaType PAGE_TYPE =
            Json.mapper().getTypeFactory().constructParametricType(Page.class, Cycle.class);

    private final WhoopHttpClient http;

    public CycleApi(WhoopHttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    /** A single cycle by id. */
    public Cycle getCycle(long cycleId) {
        return http.get("/v2/cycle/" + cycleId, Map.of(), Cycle.class);
    }

    /** One page of cycles, newest first. */
    public Page<Cycle> listCycles(PageRequest request) {
        return http.get("/v2/cycle", request.toQueryParams(), PAGE_TYPE);
    }

    /** The most recent cycles with the API's default page size. */
    public Page<Cycle> listCycles() {
        return listCycles(PageRequest.unpaged());
    }

    /**
     * Every cycle matching {@code request}, paging lazily as the stream is consumed.
     *
     * <pre>{@code
     * whoop.cycles().streamCycles(PageRequest.since(lastWeek))
     *      .filter(c -> c.scoreState().isScored())
     *      .forEach(c -> System.out.println(c.score().strain()));
     * }</pre>
     */
    public Stream<Cycle> streamCycles(PageRequest request) {
        return AutoPaging.stream(this::listCycles, request);
    }

    /** Every cycle in the user's history, newest first. */
    public Stream<Cycle> streamCycles() {
        return streamCycles(PageRequest.of(PageRequest.MAX_LIMIT));
    }

    /**
     * The sleep that falls within the given cycle.
     *
     * @throws com.whoopsdk.exception.WhoopApiException with status 404 when the cycle has no sleep
     */
    public Sleep getSleepForCycle(long cycleId) {
        return http.get("/v2/cycle/" + cycleId + "/sleep", Map.of(), Sleep.class);
    }
}
