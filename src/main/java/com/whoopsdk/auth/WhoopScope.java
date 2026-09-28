package com.whoopsdk.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/** The OAuth scopes the WHOOP authorization server understands. */
public enum WhoopScope {

    /** Name, email and user id — {@code GET /v2/user/profile/basic}. */
    READ_PROFILE("read:profile"),
    /** Height, weight and max heart rate — {@code GET /v2/user/measurement/body}. */
    READ_BODY_MEASUREMENT("read:body_measurement"),
    /** Physiological cycles — {@code GET /v2/cycle}. */
    READ_CYCLES("read:cycles"),
    /** Recovery scores — {@code GET /v2/recovery}. */
    READ_RECOVERY("read:recovery"),
    /** Sleep activities — {@code GET /v2/activity/sleep}. */
    READ_SLEEP("read:sleep"),
    /** Workouts — {@code GET /v2/activity/workout}. */
    READ_WORKOUT("read:workout"),
    /** Required to receive a refresh token; without it tokens cannot be renewed. */
    OFFLINE("offline");

    private final String value;

    WhoopScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /** Joins scopes into the space-delimited form the authorize endpoint expects. */
    public static String join(Collection<WhoopScope> scopes) {
        return scopes.stream().map(WhoopScope::value).collect(Collectors.joining(" "));
    }

    /** Every read scope plus {@code offline} — handy for development. */
    public static String all() {
        return Arrays.stream(values()).map(WhoopScope::value).collect(Collectors.joining(" "));
    }
}
