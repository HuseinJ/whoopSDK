package com.whoopsdk.auth;

import com.whoopsdk.exception.WhoopAuthException;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Keeps an access token alive by using the refresh grant whenever it nears expiry.
 *
 * <p>Requires the {@link WhoopScope#OFFLINE} scope to have been granted. WHOOP rotates refresh
 * tokens on every refresh, so pass a {@code onTokensRenewed} callback to persist the new pair —
 * otherwise a process restart loses the ability to refresh.
 *
 * <pre>{@code
 * var provider = new RefreshingTokenProvider(oauth, storedTokens, tokens -> repository.save(userId, tokens));
 * var whoop = WhoopClient.builder().tokenProvider(provider).build();
 * }</pre>
 */
public final class RefreshingTokenProvider implements AccessTokenProvider {

    /** Refresh this long before the token actually expires. */
    private static final Duration DEFAULT_LEEWAY = Duration.ofMinutes(1);

    private final WhoopOAuthClient oauth;
    private final Consumer<OAuthTokens> onTokensRenewed;
    private final Duration leeway;
    private final Object lock = new Object();

    private volatile OAuthTokens tokens;

    public RefreshingTokenProvider(WhoopOAuthClient oauth, OAuthTokens tokens) {
        this(oauth, tokens, t -> {
        }, DEFAULT_LEEWAY);
    }

    public RefreshingTokenProvider(WhoopOAuthClient oauth, OAuthTokens tokens,
                                   Consumer<OAuthTokens> onTokensRenewed) {
        this(oauth, tokens, onTokensRenewed, DEFAULT_LEEWAY);
    }

    public RefreshingTokenProvider(WhoopOAuthClient oauth, OAuthTokens tokens,
                                   Consumer<OAuthTokens> onTokensRenewed, Duration leeway) {
        this.oauth = Objects.requireNonNull(oauth, "oauth");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.onTokensRenewed = Objects.requireNonNull(onTokensRenewed, "onTokensRenewed");
        this.leeway = Objects.requireNonNull(leeway, "leeway");
    }

    /** The current token pair — read it after a refresh to persist the rotated refresh token. */
    public OAuthTokens currentTokens() {
        return tokens;
    }

    @Override
    public String accessToken() {
        OAuthTokens current = tokens;
        if (current.isExpired(leeway)) {
            return refreshLocked(current).accessToken();
        }
        return current.accessToken();
    }

    @Override
    public boolean invalidate() {
        OAuthTokens current = tokens;
        if (current.refreshToken() == null) {
            return false;
        }
        refreshLocked(current);
        return true;
    }

    private OAuthTokens refreshLocked(OAuthTokens seen) {
        synchronized (lock) {
            // Another thread may already have refreshed while we waited for the lock.
            if (tokens != seen) {
                return tokens;
            }
            String refreshToken = tokens.refreshToken();
            if (refreshToken == null) {
                throw new WhoopAuthException(
                        "Access token expired and no refresh token is available. "
                                + "Request the 'offline' scope during authorization to receive one.");
            }
            OAuthTokens renewed = oauth.refresh(refreshToken);
            // WHOOP may omit the refresh token on rotation edge cases; keep the previous one then.
            if (renewed.refreshToken() == null) {
                renewed = new OAuthTokens(renewed.accessToken(), refreshToken, renewed.expiresIn(),
                        renewed.tokenType(), renewed.scope(), renewed.obtainedAt());
            }
            tokens = renewed;
            onTokensRenewed.accept(renewed);
            return renewed;
        }
    }
}
