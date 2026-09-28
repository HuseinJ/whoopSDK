package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A recorded workout.
 *
 * @param id             unique workout id (UUID in API v2)
 * @param v1Id           the numeric id this record had in API v1; {@code null} for newer records
 * @param userId         the WHOOP user
 * @param createdAt      when the record was created
 * @param updatedAt      when the record last changed
 * @param start          workout start
 * @param end            workout end
 * @param timezoneOffset UTC offset at the time of the workout, e.g. {@code "+02:00"}
 * @param sportName      the activity, e.g. {@code "running"}
 * @param sportId        legacy numeric sport id; superseded by {@code sportName}
 * @param scoreState     whether {@code score} is populated
 * @param score          strain and heart-rate metrics, present only when {@link ScoreState#SCORED}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Workout(
        UUID id,
        Long v1Id,
        long userId,
        Instant createdAt,
        Instant updatedAt,
        Instant start,
        Instant end,
        String timezoneOffset,
        String sportName,
        Integer sportId,
        ScoreState scoreState,
        WorkoutScore score) {

    @JsonIgnore
    public Optional<WorkoutScore> scoreOptional() {
        return Optional.ofNullable(score);
    }

    @JsonIgnore
    public Duration duration() {
        return start == null || end == null ? Duration.ZERO : Duration.between(start, end);
    }
}
