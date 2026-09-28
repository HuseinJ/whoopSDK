package com.whoopsdk.http;

import com.sun.net.httpserver.HttpServer;
import com.whoopsdk.WhoopClient;
import com.whoopsdk.WhoopConfig;
import com.whoopsdk.auth.AccessTokenProvider;
import com.whoopsdk.exception.WhoopApiException;
import com.whoopsdk.exception.WhoopAuthException;
import com.whoopsdk.exception.WhoopRateLimitException;
import com.whoopsdk.model.Cycle;
import com.whoopsdk.model.PageRequest;
import com.whoopsdk.model.UserBasicProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the transport against a throwaway in-process HTTP server. */
class WhoopHttpClientTest {

    private HttpServer server;
    private String baseUrl;
    private final List<String> requestLog = new ArrayList<>();
    private final List<String> authHeaders = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private WhoopClient clientWith(AccessTokenProvider provider) {
        return WhoopClient.builder()
                .tokenProvider(provider)
                .config(WhoopConfig.builder()
                        .apiBaseUrl(baseUrl)
                        .initialRetryDelay(Duration.ofMillis(1))
                        .requestTimeout(Duration.ofSeconds(5))
                        .build())
                .build();
    }

    private void handle(String path, Handler handler) {
        server.createContext(path, exchange -> {
            requestLog.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            authHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void sendsTheBearerTokenAndParsesTheResponse() {
        handle("/v2/user/profile/basic", exchange -> respond(exchange, 200, """
                {"user_id":10129,"email":"a@b.com","first_name":"John","last_name":"Smith"}"""));

        UserBasicProfile profile = clientWith(() -> "tok-123").users().getProfile();

        assertEquals("John Smith", profile.fullName());
        assertEquals("Bearer tok-123", authHeaders.get(0));
    }

    @Test
    void serialisesPageRequestIntoTheQueryString() {
        handle("/v2/cycle", exchange -> respond(exchange, 200, "{\"records\":[]}"));

        clientWith(() -> "tok").cycles().listCycles(
                PageRequest.between(
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        java.time.Instant.parse("2026-02-01T00:00:00Z")).limit(25));

        String request = requestLog.get(0);
        assertTrue(request.contains("limit=25"), request);
        assertTrue(request.contains("start=2026-01-01T00%3A00%3A00Z"), request);
        assertTrue(request.contains("end=2026-02-01T00%3A00%3A00Z"), request);
    }

    @Test
    void retriesOnServerErrorThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        handle("/v2/cycle/1", exchange -> {
            if (attempts.incrementAndGet() < 3) {
                respond(exchange, 503, "{\"error\":\"unavailable\"}");
            } else {
                respond(exchange, 200, """
                        {"id":1,"user_id":2,"start":"2026-06-13T02:58:17.049Z","score_state":"SCORED"}""");
            }
        });

        Cycle cycle = clientWith(() -> "tok").cycles().getCycle(1);

        assertEquals(1L, cycle.id());
        assertEquals(3, attempts.get());
    }

    @Test
    void givesUpAfterTheConfiguredRetries() {
        AtomicInteger attempts = new AtomicInteger();
        handle("/v2/cycle/1", exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 500, "{\"error\":\"boom\"}");
        });

        WhoopApiException e = assertThrows(WhoopApiException.class,
                () -> clientWith(() -> "tok").cycles().getCycle(1));

        assertEquals(500, e.statusCode());
        assertEquals(4, attempts.get(), "1 initial attempt + 3 retries");
    }

    @Test
    void surfaces429AsARateLimitExceptionCarryingRetryAfter() {
        handle("/v2/cycle/1", exchange -> {
            exchange.getResponseHeaders().add("X-RateLimit-Limit", "100, 100;window=60");
            exchange.getResponseHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponseHeaders().add("X-RateLimit-Reset", "1");
            respond(exchange, 429, "{\"error\":\"rate limited\"}");
        });

        WhoopClient client = WhoopClient.builder()
                .accessToken("tok")
                .config(WhoopConfig.builder().apiBaseUrl(baseUrl).maxRetries(0).build())
                .build();

        WhoopRateLimitException e = assertThrows(WhoopRateLimitException.class,
                () -> client.cycles().getCycle(1));

        assertEquals(429, e.statusCode());
        assertEquals(Duration.ofSeconds(1), e.retryAfter().orElseThrow());
        assertEquals(0, client.lastRateLimit().orElseThrow().remainingRequests().orElseThrow());
    }

    @Test
    void refreshesTheTokenOnceAfterA401() {
        AtomicInteger attempts = new AtomicInteger();
        handle("/v2/user/profile/basic", exchange -> {
            if (attempts.incrementAndGet() == 1) {
                respond(exchange, 401, "{\"error\":\"expired\"}");
            } else {
                respond(exchange, 200, """
                        {"user_id":1,"email":"a@b.com","first_name":"A","last_name":"B"}""");
            }
        });

        AtomicInteger refreshes = new AtomicInteger();
        AccessTokenProvider provider = new AccessTokenProvider() {
            @Override
            public String accessToken() {
                return refreshes.get() == 0 ? "stale" : "fresh";
            }

            @Override
            public boolean invalidate() {
                refreshes.incrementAndGet();
                return true;
            }
        };

        clientWith(provider).users().getProfile();

        assertEquals(1, refreshes.get());
        assertEquals("Bearer stale", authHeaders.get(0));
        assertEquals("Bearer fresh", authHeaders.get(1));
    }

    @Test
    void throwsAuthExceptionWhenTheTokenCannotBeRefreshed() {
        handle("/v2/user/profile/basic", exchange -> respond(exchange, 401, "{\"error\":\"nope\"}"));

        assertThrows(WhoopAuthException.class, () -> clientWith(() -> "tok").users().getProfile());
    }

    @Test
    void doesNotRetryA404() {
        AtomicInteger attempts = new AtomicInteger();
        handle("/v2/cycle/99", exchange -> {
            attempts.incrementAndGet();
            respond(exchange, 404, "{\"error\":\"not found\"}");
        });

        WhoopApiException e = assertThrows(WhoopApiException.class,
                () -> clientWith(() -> "tok").cycles().getCycle(99));

        assertTrue(e.isNotFound());
        assertEquals(1, attempts.get());
    }

    @Test
    void streamsAcrossPages() {
        handle("/v2/recovery", exchange -> {
            boolean firstPage = exchange.getRequestURI().getQuery() == null
                    || !exchange.getRequestURI().getQuery().contains("nextToken");
            if (firstPage) {
                respond(exchange, 200, """
                        {"records":[{"cycle_id":1,"user_id":9,"score_state":"SCORED"}],"next_token":"t1"}""");
            } else {
                respond(exchange, 200, """
                        {"records":[{"cycle_id":2,"user_id":9,"score_state":"SCORED"}]}""");
            }
        });

        List<Long> cycleIds = clientWith(() -> "tok").recoveries()
                .streamRecoveries(PageRequest.of(1))
                .map(r -> r.cycleId())
                .toList();

        assertEquals(List.of(1L, 2L), cycleIds);
        assertEquals(2, requestLog.size());
        assertTrue(requestLog.get(1).contains("nextToken=t1"), requestLog.get(1));
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
