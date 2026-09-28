package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A sleep activity — either a full night or a nap.
 *
 * @param id             unique sleep id (UUID in API v2)
 * @param v1Id           the numeric id this record had in API v1; {@code null} for newer records
 * @param cycleId        the cycle this sleep belongs to
 * @param userId         the WHOOP user
 * @param createdAt      when the record was created
 * @param updatedAt      when the record last changed
 * @param start          when the user fell asleep
 * @param end            when the user woke up
 * @param timezoneOffset UTC offset at the time of the sleep, e.g. {@code "+02:00"}
 * @param nap            {@code true} for naps, {@code false} for the main sleep
 * @param scoreState     whether {@code score} is populated
 * @param score          sleep stages and performance, present only when {@link ScoreState#SCORED}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Sleep(
        UUID id,
        Long v1Id,
        long cycleId,
        long userId,
        Instant createdAt,
        Instant updatedAt,
        Instant start,
        Instant end,
        String timezoneOffset,
        boolean nap,
        ScoreState scoreState,
        SleepScore score) {

    @JsonIgnore
    public Optional<SleepScore> scoreOptional() {
        return Optional.ofNullable(score);
    }

    /** Wall-clock time between falling asleep and waking, including time awake in bed. */
    @JsonIgnore
    public Duration timeInBed() {
        return start == null || end == null ? Duration.ZERO : Duration.between(start, end);
    }
}
