package com.whoopsdk.auth;

import com.whoopsdk.WhoopConfig;
import com.whoopsdk.exception.WhoopAuthException;
import com.whoopsdk.http.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Drives the OAuth 2.0 authorization-code flow against WHOOP.
 *
 * <p>Typical server-side sequence:
 * <pre>{@code
 * var oauth = new WhoopOAuthClient(clientId, clientSecret);
 *
 * // 1. Send the user to WHOOP.
 * String state = WhoopOAuthClient.randomState();      // persist against the session
 * String url = oauth.authorizationUrl(redirectUri, Set.of(READ_RECOVERY, OFFLINE), state);
 *
 * // 2. WHOOP redirects back to redirectUri?code=...&state=...  — verify state, then:
 * OAuthTokens tokens = oauth.exchangeCode(code, redirectUri);
 *
 * // 3. Later, when the access token is close to expiry:
 * OAuthTokens renewed = oauth.refresh(tokens.refreshToken());
 * }</pre>
 *
 * <p>WHOOP rotates refresh tokens: always persist the refresh token from the newest response.
 */
public final class WhoopOAuthClient {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String clientId;
    private final String clientSecret;
    private final WhoopConfig config;

    public WhoopOAuthClient(String clientId, String clientSecret) {
        this(clientId, clientSecret, WhoopConfig.defaults());
    }

    public WhoopOAuthClient(String clientId, String clientSecret, WhoopConfig config) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
        this.config = Objects.requireNonNull(config, "config");
    }

    /** A cryptographically random {@code state} value for CSRF protection. */
    public static String randomState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Builds the URL to redirect the user to.
     *
     * <p>WHOOP requires a {@code state} of at least 8 characters.
     */
    public String authorizationUrl(String redirectUri, Collection<WhoopScope> scopes, String state) {
        return authorizationUrl(redirectUri, WhoopScope.join(scopes), state);
    }

    /** Same as above but with scopes already joined into a space-delimited string. */
    public String authorizationUrl(String redirectUri, String scope, String state) {
        Objects.requireNonNull(redirectUri, "redirectUri");
        Objects.requireNonNull(state, "state");
        if (state.length() < 8) {
            throw new IllegalArgumentException("WHOOP requires a state value of at least 8 characters");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("scope", scope);
        params.put("state", state);
        return config.authorizeUrl() + "?" + form(params);
    }

    /** Exchanges the {@code code} from the redirect for an access token. */
    public OAuthTokens exchangeCode(String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", Objects.requireNonNull(code, "code"));
        form.put("redirect_uri", Objects.requireNonNull(redirectUri, "redirectUri"));
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        return post(form);
    }

    /**
     * Exchanges a refresh token for a new access token.
     *
     * <p>The response carries a new refresh token — store it, the old one is spent.
     */
    public OAuthTokens refresh(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", Objects.requireNonNull(refreshToken, "refreshToken"));
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("scope", "offline");
        return post(form);
    }

    private OAuthTokens post(Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.tokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("User-Agent", config.userAgent())
                .timeout(config.requestTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(form(form), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = config.httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WhoopAuthException("Token request to WHOOP failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhoopAuthException("Token request to WHOOP was interrupted", e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new WhoopAuthException("WHOOP token endpoint returned " + response.statusCode()
                    + ": " + response.body());
        }

        OAuthTokens tokens = Json.read(response.body(), OAuthTokens.class);
        if (tokens.accessToken() == null) {
            throw new WhoopAuthException("WHOOP token response contained no access_token: " + response.body());
        }
        return tokens.withObtainedAt(Instant.now());
    }

    private static String form(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
