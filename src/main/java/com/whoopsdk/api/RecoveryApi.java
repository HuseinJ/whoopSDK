package com.whoopsdk.api;

import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.http.Json;
import com.whoopsdk.http.WhoopHttpClient;
import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;
import com.whoopsdk.model.Recovery;
import com.whoopsdk.pagination.AutoPaging;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Recovery scores — {@code /v2/recovery} and {@code /v2/cycle/{id}/recovery}.
 * Reached via {@code whoop.recoveries()}.
 *
 * <p>All methods require the {@code read:recovery} scope.
 */
public final class RecoveryApi {

    private static final JavaType PAGE_TYPE =
            Json.mapper().getTypeFactory().constructParametricType(Page.class, Recovery.class);

    private final WhoopHttpClient http;

    public RecoveryApi(WhoopHttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    /** One page of recoveries, newest first. */
    public Page<Recovery> listRecoveries(PageRequest request) {
        return http.get("/v2/recovery", request.toQueryParams(), PAGE_TYPE);
    }

    /** The most recent recoveries with the API's default page size. */
    public Page<Recovery> listRecoveries() {
        return listRecoveries(PageRequest.unpaged());
    }

    /** Every recovery matching {@code request}, paging lazily as the stream is consumed. */
    public Stream<Recovery> streamRecoveries(PageRequest request) {
        return AutoPaging.stream(this::listRecoveries, request);
    }

    /** Every recovery in the user's history, newest first. */
    public Stream<Recovery> streamRecoveries() {
        return streamRecoveries(PageRequest.of(PageRequest.MAX_LIMIT));
    }

    /**
     * The recovery scored for a specific cycle.
     *
     * @throws com.whoopsdk.exception.WhoopApiException with status 404 when the cycle has no
     *                                                  recovery yet
     */
    public Recovery getRecoveryForCycle(long cycleId) {
        return http.get("/v2/cycle/" + cycleId + "/recovery", Map.of(), Recovery.class);
    }
}
