package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Optional;

/**
 * A physiological cycle — WHOOP's "day", which runs from wake to wake rather than midnight to
 * midnight.
 *
 * @param id             unique cycle id
 * @param userId         the WHOOP user this cycle belongs to
 * @param createdAt      when the record was created
 * @param updatedAt      when the record last changed
 * @param start          start of the cycle
 * @param end            end of the cycle; {@code null} while the cycle is still ongoing
 * @param timezoneOffset UTC offset at the time of the cycle, e.g. {@code "+02:00"}
 * @param scoreState     whether {@code score} is populated
 * @param score          strain and heart-rate summary, present only when {@link ScoreState#SCORED}
 * @param stepCount      steps recorded during the cycle, when the device reports them
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cycle(
        long id,
        long userId,
        Instant createdAt,
        Instant updatedAt,
        Instant start,
        Instant end,
        String timezoneOffset,
        ScoreState scoreState,
        CycleScore score,
        Integer stepCount) {

    /** The score, absent unless {@link #scoreState()} is {@link ScoreState#SCORED}. */
    @JsonIgnore
    public Optional<CycleScore> scoreOptional() {
        return Optional.ofNullable(score);
    }

    /** {@code true} while this cycle is still running. */
    @JsonIgnore
    public boolean isOngoing() {
        return end == null;
    }
}
