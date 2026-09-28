package com.whoopsdk.exception;

import java.util.Map;
import java.util.Optional;

/** Thrown when the WHOOP API answers with a non-2xx status code. */
public class WhoopApiException extends WhoopException {

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    public WhoopApiException(int statusCode, String body, Map<String, String> headers) {
        super("WHOOP API request failed with status " + statusCode
                + (body == null || body.isBlank() ? "" : ": " + truncate(body)));
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public int statusCode() {
        return statusCode;
    }

    /** Raw response body, useful for logging the API's error payload. */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Map<String, String> headers() {
        return headers;
    }

    /** {@code true} for 404 responses, the common "resource does not exist" case. */
    public boolean isNotFound() {
        return statusCode == 404;
    }

    private static String truncate(String s) {
        return s.length() <= 512 ? s : s.substring(0, 512) + "…";
    }
}
