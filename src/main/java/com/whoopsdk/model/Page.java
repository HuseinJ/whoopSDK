package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

/**
 * One page of a paginated collection endpoint.
 *
 * <p>Prefer the {@code stream*} methods on the API classes, which walk pages for you.
 *
 * @param <T>       the record type, e.g. {@link Cycle} or {@link Workout}
 * @param records   the records on this page
 * @param nextToken token to pass as {@code nextToken} to fetch the following page; absent on the
 *                  last page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Page<T>(List<T> records, String nextToken) {

    public Page {
        records = records == null ? List.of() : List.copyOf(records);
    }

    @JsonIgnore
    public Optional<String> nextTokenOptional() {
        return Optional.ofNullable(nextToken);
    }

    /** {@code true} when another page is available. */
    @JsonIgnore
    public boolean hasNext() {
        return nextToken != null && !nextToken.isBlank();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return records.isEmpty();
    }

    @JsonIgnore
    public int size() {
        return records.size();
    }
}
