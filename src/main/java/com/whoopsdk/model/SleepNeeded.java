package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;

/**
 * Breakdown of how much sleep WHOOP calculated the user needed, in milliseconds.
 *
 * @param baselineMilli               the user's habitual sleep need
 * @param needFromSleepDebtMilli      extra need accrued from previous shortfalls
 * @param needFromRecentStrainMilli   extra need from recent strain
 * @param needFromRecentNapMilli      reduction (negative) for sleep already obtained in naps
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SleepNeeded(
        long baselineMilli,
        long needFromSleepDebtMilli,
        long needFromRecentStrainMilli,
        long needFromRecentNapMilli) {

    /** Total sleep need: baseline plus debt plus strain, less any recent nap. */
    @JsonIgnore
    public Duration total() {
        return Duration.ofMillis(
                baselineMilli + needFromSleepDebtMilli + needFromRecentStrainMilli + needFromRecentNapMilli);
    }

    @JsonIgnore
    public Duration baseline() {
        return Duration.ofMillis(baselineMilli);
    }
}
