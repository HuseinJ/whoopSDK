package com.whoopsdk.api;

import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.http.Json;
import com.whoopsdk.http.WhoopHttpClient;
import com.whoopsdk.model.Page;
import com.whoopsdk.model.PageRequest;
import com.whoopsdk.model.Workout;
import com.whoopsdk.pagination.AutoPaging;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Workouts — endpoints under {@code /v2/activity/workout}. Reached via {@code whoop.workouts()}.
 *
 * <p>All methods require the {@code read:workout} scope.
 */
public final class WorkoutApi {

    private static final JavaType PAGE_TYPE =
            Json.mapper().getTypeFactory().constructParametricType(Page.class, Workout.class);

    private final WhoopHttpClient http;

    public WorkoutApi(WhoopHttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    /** A single workout by its v2 UUID. */
    public Workout getWorkout(UUID workoutId) {
        return http.get("/v2/activity/workout/" + workoutId, Map.of(), Workout.class);
    }

    /** A single workout by its v2 UUID in string form. */
    public Workout getWorkout(String workoutId) {
        return getWorkout(UUID.fromString(workoutId));
    }

    /** One page of workouts, newest first. */
    public Page<Workout> listWorkouts(PageRequest request) {
        return http.get("/v2/activity/workout", request.toQueryParams(), PAGE_TYPE);
    }

    /** The most recent workouts with the API's default page size. */
    public Page<Workout> listWorkouts() {
        return listWorkouts(PageRequest.unpaged());
    }

    /** Every workout matching {@code request}, paging lazily as the stream is consumed. */
    public Stream<Workout> streamWorkouts(PageRequest request) {
        return AutoPaging.stream(this::listWorkouts, request);
    }

    /** Every workout in the user's history, newest first. */
    public Stream<Workout> streamWorkouts() {
        return streamWorkouts(PageRequest.of(PageRequest.MAX_LIMIT));
    }

    /**
     * Resolves a v1 numeric activity id to its v2 UUID.
     *
     * <p>Only needed when migrating data captured against API v1.
     */
    public UUID resolveV1WorkoutId(long v1ActivityId) {
        String body = http.request("GET", "/v1/activity-mapping/" + v1ActivityId, Map.of(), null);
        ActivityMapping mapping = Json.read(body, ActivityMapping.class);
        return mapping.id();
    }

    private record ActivityMapping(UUID id) {
    }
}
