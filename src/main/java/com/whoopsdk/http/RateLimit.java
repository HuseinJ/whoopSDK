package com.whoopsdk.http;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Snapshot of the rate-limit headers returned with the most recent response.
 *
 * @param limit     raw {@code X-RateLimit-Limit} value, e.g. {@code "100, 100;window=60, 10000;window=86400"}
 * @param remaining requests left in the current window, when reported
 * @param reset     time until the window resets, when reported
 */
public record RateLimit(String limit, Integer remaining, Duration reset) {

    public static final String HEADER_LIMIT = "x-ratelimit-limit";
    public static final String HEADER_REMAINING = "x-ratelimit-remaining";
    public static final String HEADER_RESET = "x-ratelimit-reset";

    /** Parses the rate-limit headers out of a (lower-cased key) header map. */
    public static Optional<RateLimit> from(Map<String, String> headers) {
        String limit = headers.get(HEADER_LIMIT);
        Integer remaining = parseInt(headers.get(HEADER_REMAINING));
        Duration reset = parseSeconds(headers.get(HEADER_RESET));
        if (limit == null && remaining == null && reset == null) {
            return Optional.empty();
        }
        return Optional.of(new RateLimit(limit, remaining, reset));
    }

    public Optional<Integer> remainingRequests() {
        return Optional.ofNullable(remaining);
    }

    public Optional<Duration> resetIn() {
        return Optional.ofNullable(reset);
    }

    private static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Duration parseSeconds(String value) {
        Integer seconds = parseInt(value);
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }
}
