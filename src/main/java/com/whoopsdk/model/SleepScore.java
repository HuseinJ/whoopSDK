package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

/**
 * Scoring for a {@link Sleep}.
 *
 * @param stageSummary                 time spent in each sleep stage
 * @param sleepNeeded                  how much sleep WHOOP calculated the user needed
 * @param respiratoryRate              breaths per minute during sleep
 * @param sleepPerformancePercentage   sleep obtained vs. sleep needed, 0–100
 * @param sleepConsistencyPercentage   how closely bed/wake times matched recent days, 0–100
 * @param sleepEfficiencyPercentage    time asleep vs. time in bed, 0–100
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SleepScore(
        SleepStageSummary stageSummary,
        SleepNeeded sleepNeeded,
        Float respiratoryRate,
        Float sleepPerformancePercentage,
        Float sleepConsistencyPercentage,
        Float sleepEfficiencyPercentage) {

    @JsonIgnore
    public Optional<Float> performance() {
        return Optional.ofNullable(sleepPerformancePercentage);
    }

    @JsonIgnore
    public Optional<Float> efficiency() {
        return Optional.ofNullable(sleepEfficiencyPercentage);
    }
}
