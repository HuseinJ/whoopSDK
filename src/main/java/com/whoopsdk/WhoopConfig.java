package com.whoopsdk;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * Transport-level settings shared by every API group.
 *
 * <p>The defaults target WHOOP production and are what you want unless you are pointing the SDK at
 * a mock server in tests.
 */
public final class WhoopConfig {

    public static final String DEFAULT_API_BASE_URL = "https://api.prod.whoop.com/developer";
    public static final String DEFAULT_AUTHORIZE_URL = "https://api.prod.whoop.com/oauth/oauth2/auth";
    public static final String DEFAULT_TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";

    private final String apiBaseUrl;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration initialRetryDelay;
    private final Duration maxRetryDelay;
    private final String userAgent;
    private final HttpClient httpClient;

    private WhoopConfig(Builder b) {
        this.apiBaseUrl = stripTrailingSlash(b.apiBaseUrl);
        this.authorizeUrl = b.authorizeUrl;
        this.tokenUrl = b.tokenUrl;
        this.connectTimeout = b.connectTimeout;
        this.requestTimeout = b.requestTimeout;
        this.maxRetries = b.maxRetries;
        this.initialRetryDelay = b.initialRetryDelay;
        this.maxRetryDelay = b.maxRetryDelay;
        this.userAgent = b.userAgent;
        this.httpClient = b.httpClient != null ? b.httpClient : HttpClient.newBuilder()
                .connectTimeout(b.connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Production defaults. */
    public static WhoopConfig defaults() {
        return builder().build();
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    public String authorizeUrl() {
        return authorizeUrl;
    }

    public String tokenUrl() {
        return tokenUrl;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Duration initialRetryDelay() {
        return initialRetryDelay;
    }

    public Duration maxRetryDelay() {
        return maxRetryDelay;
    }

    public String userAgent() {
        return userAgent;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static final class Builder {
        private String apiBaseUrl = DEFAULT_API_BASE_URL;
        private String authorizeUrl = DEFAULT_AUTHORIZE_URL;
        private String tokenUrl = DEFAULT_TOKEN_URL;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private Duration initialRetryDelay = Duration.ofMillis(500);
        private Duration maxRetryDelay = Duration.ofSeconds(30);
        private String userAgent = "whoop-java-sdk/0.1.0";
        private HttpClient httpClient;

        /** Override the API root, e.g. to point at a WireMock server in tests. */
        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl);
            return this;
        }

        public Builder authorizeUrl(String authorizeUrl) {
            this.authorizeUrl = Objects.requireNonNull(authorizeUrl);
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = Objects.requireNonNull(tokenUrl);
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout);
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout);
            return this;
        }

        /** Retries for 429 and 5xx responses. {@code 0} disables retrying. */
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialRetryDelay(Duration initialRetryDelay) {
            this.initialRetryDelay = Objects.requireNonNull(initialRetryDelay);
            return this;
        }

        public Builder maxRetryDelay(Duration maxRetryDelay) {
            this.maxRetryDelay = Objects.requireNonNull(maxRetryDelay);
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent);
            return this;
        }

        /** Supply your own {@link HttpClient} (proxy, executor, TLS context, …). */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public WhoopConfig build() {
            return new WhoopConfig(this);
        }
    }
}
