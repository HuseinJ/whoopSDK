package com.whoopsdk;

import com.whoopsdk.api.CycleApi;
import com.whoopsdk.api.RecoveryApi;
import com.whoopsdk.api.SleepApi;
import com.whoopsdk.api.UserApi;
import com.whoopsdk.api.WorkoutApi;
import com.whoopsdk.auth.AccessTokenProvider;
import com.whoopsdk.auth.StaticTokenProvider;
import com.whoopsdk.http.RateLimit;
import com.whoopsdk.http.WhoopHttpClient;

import java.util.Objects;
import java.util.Optional;

/**
 * Entry point to the WHOOP Developer Platform (API v2).
 *
 * <p>One client acts on behalf of one authorized user. It is thread-safe and cheap to keep around
 * for the lifetime of that user's session — build one per user, not one per request.
 *
 * <pre>{@code
 * WhoopClient whoop = WhoopClient.withAccessToken(token);
 *
 * UserBasicProfile me = whoop.users().getProfile();
 * whoop.recoveries().streamRecoveries(PageRequest.since(lastMonth))
 *      .forEach(r -> log.info("{}: {}%", r.cycleId(), r.score().recoveryScore()));
 * }</pre>
 *
 * <p>For tokens that must survive longer than an hour, pass a
 * {@link com.whoopsdk.auth.RefreshingTokenProvider} instead of a raw token.
 */
public final class WhoopClient {

    private final WhoopConfig config;
    private final WhoopHttpClient http;
    private final UserApi users;
    private final CycleApi cycles;
    private final RecoveryApi recoveries;
    private final SleepApi sleeps;
    private final WorkoutApi workouts;

    private WhoopClient(WhoopConfig config, AccessTokenProvider tokenProvider) {
        this.config = config;
        this.http = new WhoopHttpClient(config, tokenProvider);
        this.users = new UserApi(http);
        this.cycles = new CycleApi(http);
        this.recoveries = new RecoveryApi(http);
        this.sleeps = new SleepApi(http);
        this.workouts = new WorkoutApi(http);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Shorthand for a client backed by a token you already hold. */
    public static WhoopClient withAccessToken(String accessToken) {
        return builder().accessToken(accessToken).build();
    }

    /** Profile, body measurements, and access revocation. */
    public UserApi users() {
        return users;
    }

    /** Physiological cycles, and the sleep inside a cycle. */
    public CycleApi cycles() {
        return cycles;
    }

    /** Recovery scores. */
    public RecoveryApi recoveries() {
        return recoveries;
    }

    /** Sleeps and naps. */
    public SleepApi sleeps() {
        return sleeps;
    }

    /** Workouts. */
    public WorkoutApi workouts() {
        return workouts;
    }

    /**
     * Rate-limit headers from the most recent response.
     *
     * <p>WHOOP allows 100 requests/minute and 10,000/day per client. Use this to back off before
     * you hit the ceiling on bulk imports.
     */
    public Optional<RateLimit> lastRateLimit() {
        return http.lastRateLimit();
    }

    public WhoopConfig config() {
        return config;
    }

    /** Escape hatch for endpoints this SDK does not wrap yet. */
    public WhoopHttpClient http() {
        return http;
    }

    public static final class Builder {
        private WhoopConfig config = WhoopConfig.defaults();
        private AccessTokenProvider tokenProvider;

        /** Use a fixed token. Mutually exclusive with {@link #tokenProvider}. */
        public Builder accessToken(String accessToken) {
            this.tokenProvider = new StaticTokenProvider(accessToken);
            return this;
        }

        /**
         * Use a provider that can renew the token — typically
         * {@link com.whoopsdk.auth.RefreshingTokenProvider}.
         */
        public Builder tokenProvider(AccessTokenProvider tokenProvider) {
            this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
            return this;
        }

        /** Timeouts, retry policy, base URLs. */
        public Builder config(WhoopConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        public WhoopClient build() {
            if (tokenProvider == null) {
                throw new IllegalStateException(
                        "No credentials: call accessToken(..) or tokenProvider(..) before build()");
            }
            return new WhoopClient(config, tokenProvider);
        }
    }
}
