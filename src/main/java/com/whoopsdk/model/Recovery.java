package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * How recovered the user is at the start of a {@link Cycle}, derived from the preceding sleep.
 *
 * @param cycleId    the cycle this recovery scores
 * @param sleepId    the sleep the score was computed from
 * @param userId     the WHOOP user
 * @param createdAt  when the record was created
 * @param updatedAt  when the record last changed
 * @param scoreState whether {@code score} is populated
 * @param score      the recovery metrics, present only when {@link ScoreState#SCORED}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Recovery(
        long cycleId,
        UUID sleepId,
        long userId,
        Instant createdAt,
        Instant updatedAt,
        ScoreState scoreState,
        RecoveryScore score) {

    @JsonIgnore
    public Optional<RecoveryScore> scoreOptional() {
        return Optional.ofNullable(score);
    }
}
