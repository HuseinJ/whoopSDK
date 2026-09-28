package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;

/**
 * Time spent in each sleep stage, in milliseconds as returned by the API.
 *
 * <p>The {@code *Duration()} helpers convert to {@link Duration} for arithmetic.
 *
 * @param totalInBedTimeMilli        total time in bed
 * @param totalAwakeTimeMilli        time awake while in bed
 * @param totalNoDataTimeMilli       time the strap recorded nothing (e.g. it was off)
 * @param totalLightSleepTimeMilli   light sleep
 * @param totalSlowWaveSleepTimeMilli slow-wave (deep) sleep
 * @param totalRemSleepTimeMilli     REM sleep
 * @param sleepCycleCount            number of sleep cycles completed
 * @param disturbanceCount           number of times sleep was disturbed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SleepStageSummary(
        int totalInBedTimeMilli,
        int totalAwakeTimeMilli,
        int totalNoDataTimeMilli,
        int totalLightSleepTimeMilli,
        int totalSlowWaveSleepTimeMilli,
        int totalRemSleepTimeMilli,
        int sleepCycleCount,
        int disturbanceCount) {

    @JsonIgnore
    public Duration inBed() {
        return Duration.ofMillis(totalInBedTimeMilli);
    }

    @JsonIgnore
    public Duration awake() {
        return Duration.ofMillis(totalAwakeTimeMilli);
    }

    @JsonIgnore
    public Duration lightSleep() {
        return Duration.ofMillis(totalLightSleepTimeMilli);
    }

    @JsonIgnore
    public Duration deepSleep() {
        return Duration.ofMillis(totalSlowWaveSleepTimeMilli);
    }

    @JsonIgnore
    public Duration remSleep() {
        return Duration.ofMillis(totalRemSleepTimeMilli);
    }

    /** Light + deep + REM, i.e. time in bed minus awake and no-data time. */
    @JsonIgnore
    public Duration totalAsleep() {
        return lightSleep().plus(deepSleep()).plus(remSleep());
    }
}
