package com.whoopsdk.exception;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Thrown on HTTP 429 after the configured retries have been exhausted.
 *
 * <p>WHOOP enforces 100 requests/minute and 10,000 requests/day per client.
 */
public class WhoopRateLimitException extends WhoopApiException {

    private final Duration retryAfter;

    public WhoopRateLimitException(int statusCode, String body, Map<String, String> headers, Duration retryAfter) {
        super(statusCode, body, headers);
        this.retryAfter = retryAfter;
    }

    /** How long the caller should wait before retrying, when the API told us. */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
