package com.whoopsdk.auth;

import java.util.Objects;

/** A fixed access token. Simplest option when something else owns the OAuth lifecycle. */
public final class StaticTokenProvider implements AccessTokenProvider {

    private final String token;

    public StaticTokenProvider(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    @Override
    public String accessToken() {
        return token;
    }
}
