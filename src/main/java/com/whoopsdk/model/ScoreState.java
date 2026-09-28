package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether WHOOP has produced a score for a record.
 *
 * <p>Only {@link #SCORED} records carry a non-null {@code score}.
 */
public enum ScoreState {

    /** The score is present and final. */
    SCORED,
    /** WHOOP has the data but has not finished scoring it yet; check back later. */
    PENDING_SCORE,
    /** Not enough data was recorded to produce a score; it will never arrive. */
    UNSCORABLE,
    /** A value this SDK version does not know about. */
    UNKNOWN;

    @JsonCreator
    public static ScoreState fromJson(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (ScoreState state : values()) {
            if (state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        return UNKNOWN;
    }

    public boolean isScored() {
        return this == SCORED;
    }
}
