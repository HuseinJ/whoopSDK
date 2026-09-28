package com.whoopsdk.auth;

/**
 * Supplies the bearer token sent with every API call.
 *
 * <p>Implementations must be thread-safe: a single {@link com.whoopsdk.WhoopClient} may be shared
 * across threads.
 *
 * @see StaticTokenProvider for a token you already hold
 * @see RefreshingTokenProvider for tokens that renew themselves via the refresh grant
 */
@FunctionalInterface
public interface AccessTokenProvider {

    /** Returns a currently valid access token, refreshing it if the implementation supports that. */
    String accessToken();

    /**
     * Called by the SDK when the API rejects the supplied token with 401, giving the provider a
     * chance to discard its cached value so the next {@link #accessToken()} fetches a fresh one.
     *
     * @return {@code true} if a retry with a new token is worth attempting
     */
    default boolean invalidate() {
        return false;
    }
}
