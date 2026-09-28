package com.whoopsdk.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.whoopsdk.WhoopConfig;
import com.whoopsdk.auth.AccessTokenProvider;
import com.whoopsdk.exception.WhoopApiException;
import com.whoopsdk.exception.WhoopAuthException;
import com.whoopsdk.exception.WhoopException;
import com.whoopsdk.exception.WhoopRateLimitException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The single point where HTTP happens.
 *
 * <p>Adds the bearer token, retries 429/5xx with exponential backoff (honouring
 * {@code X-RateLimit-Reset} and {@code Retry-After}), retries once on 401 after asking the token
 * provider to refresh, and maps failures onto the SDK exception hierarchy.
 */
public final class WhoopHttpClient {

    private final WhoopConfig config;
    private final AccessTokenProvider tokenProvider;

    private volatile RateLimit lastRateLimit;

    public WhoopHttpClient(WhoopConfig config, AccessTokenProvider tokenProvider) {
        this.config = Objects.requireNonNull(config, "config");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
    }

    /** Rate-limit headers from the most recent response, if the API sent any. */
    public Optional<RateLimit> lastRateLimit() {
        return Optional.ofNullable(lastRateLimit);
    }

    public <T> T get(String path, Map<String, String> query, Class<T> type) {
        return Json.read(request("GET", path, query, null), type);
    }

    public <T> T get(String path, Map<String, String> query, TypeReference<T> type) {
        JavaType javaType = Json.mapper().getTypeFactory().constructType(type);
        return Json.read(request("GET", path, query, null), javaType);
    }

    public <T> T get(String path, Map<String, String> query, JavaType type) {
        return Json.read(request("GET", path, query, null), type);
    }

    /** Fires a request whose response body is not needed (or is empty). */
    public void delete(String path) {
        request("DELETE", path, Map.of(), null);
    }

    /** Executes a request and returns the raw body. */
    public String request(String method, String path, Map<String, String> query, String body) {
        URI uri = buildUri(path, query);
        boolean retriedUnauthorized = false;
        int attempt = 0;

        while (true) {
            HttpResponse<String> response = send(method, uri, body);
            Map<String, String> headers = headerMap(response);
            RateLimit.from(headers).ifPresent(rl -> this.lastRateLimit = rl);

            int status = response.statusCode();
            if (status / 100 == 2) {
                return response.body();
            }

            if (status == 401 && !retriedUnauthorized && tokenProvider.invalidate()) {
                retriedUnauthorized = true;
                continue;
            }
            if (status == 401 || status == 403) {
                throw new WhoopAuthException("WHOOP rejected the request with status " + status
                        + ". Check that the token is valid and carries the required scope. Body: "
                        + response.body());
            }

            boolean retryable = status == 429 || status >= 500;
            if (retryable && attempt < config.maxRetries()) {
                sleep(backoff(attempt, headers));
                attempt++;
                continue;
            }

            if (status == 429) {
                throw new WhoopRateLimitException(status, response.body(), headers,
                        retryAfter(headers).orElse(null));
            }
            throw new WhoopApiException(status, response.body(), headers);
        }
    }

    private HttpResponse<String> send(String method, URI uri, String body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + tokenProvider.accessToken())
                .header("Accept", "application/json")
                .header("User-Agent", config.userAgent())
                .timeout(config.requestTimeout())
                .method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        try {
            return config.httpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WhoopException("HTTP call to " + uri + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhoopException("HTTP call to " + uri + " was interrupted", e);
        }
    }

    private URI buildUri(String path, Map<String, String> query) {
        StringBuilder url = new StringBuilder(config.apiBaseUrl());
        if (!path.startsWith("/")) {
            url.append('/');
        }
        url.append(path);

        Map<String, String> params = new LinkedHashMap<>();
        if (query != null) {
            query.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    params.put(k, v);
                }
            });
        }
        if (!params.isEmpty()) {
            url.append('?').append(params.entrySet().stream()
                    .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                    .collect(Collectors.joining("&")));
        }
        return URI.create(url.toString());
    }

    /** Exponential backoff with the server's hint taking precedence when present. */
    private Duration backoff(int attempt, Map<String, String> headers) {
        Duration hinted = retryAfter(headers).orElse(null);
        if (hinted != null) {
            return cap(hinted);
        }
        long millis = config.initialRetryDelay().toMillis() * (1L << attempt);
        return cap(Duration.ofMillis(millis));
    }

    private Duration cap(Duration d) {
        return d.compareTo(config.maxRetryDelay()) > 0 ? config.maxRetryDelay() : d;
    }

    private static Optional<Duration> retryAfter(Map<String, String> headers) {
        String retryAfter = headers.get("retry-after");
        if (retryAfter != null) {
            try {
                return Optional.of(Duration.ofSeconds(Long.parseLong(retryAfter.trim())));
            } catch (NumberFormatException ignored) {
                // Retry-After may also be an HTTP date; fall through to the rate-limit header.
            }
        }
        return RateLimit.from(headers).flatMap(RateLimit::resetIn);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(0, duration.toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhoopException("Interrupted while backing off before a retry", e);
        }
    }

    private static Map<String, String> headerMap(HttpResponse<?> response) {
        Map<String, String> map = new HashMap<>();
        response.headers().map().forEach((key, values) -> {
            if (!values.isEmpty()) {
                map.put(key.toLowerCase(Locale.ROOT), values.get(0));
            }
        });
        return map;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
