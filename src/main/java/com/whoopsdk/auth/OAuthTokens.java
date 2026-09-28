package com.whoopsdk.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The token-endpoint response.
 *
 * <p>{@code refreshToken} is only present when the {@code offline} scope was requested during
 * authorization — without it the access token cannot be renewed and the user must re-authorize.
 *
 * @param accessToken  bearer token for API calls
 * @param refreshToken token used to obtain the next access token, when {@code offline} was granted
 * @param expiresIn    lifetime of the access token in seconds
 * @param tokenType    always {@code "bearer"} in practice
 * @param scope        space-delimited scopes actually granted
 * @param obtainedAt   when this SDK received the response; used to compute {@link #expiresAt()}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthTokens(
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String tokenType,
        String scope,
        Instant obtainedAt) {

    /** Stamps {@code obtainedAt} if the deserialized payload did not carry one. */
    public OAuthTokens withObtainedAt(Instant instant) {
        return new OAuthTokens(accessToken, refreshToken, expiresIn, tokenType, scope, instant);
    }

    @JsonIgnore
    public Optional<String> refreshTokenOptional() {
        return Optional.ofNullable(refreshToken);
    }

    /** Absolute expiry, derived from {@link #obtainedAt()} plus {@link #expiresIn()}. */
    @JsonIgnore
    public Optional<Instant> expiresAt() {
        if (expiresIn == null || obtainedAt == null) {
            return Optional.empty();
        }
        return Optional.of(obtainedAt.plusSeconds(expiresIn));
    }

    /**
     * Whether the token is expired, or close enough that it should be renewed now.
     *
     * @param leeway safety margin subtracted from the expiry, e.g. one minute
     */
    @JsonIgnore
    public boolean isExpired(Duration leeway) {
        return expiresAt()
                .map(exp -> Instant.now().isAfter(exp.minus(leeway)))
                .orElse(false);
    }

    @JsonIgnore
    public List<String> scopes() {
        return scope == null || scope.isBlank() ? List.of() : List.of(scope.trim().split("\\s+"));
    }
}
