package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.util.List;

/**
 * Milliseconds spent in each heart-rate zone during a workout.
 *
 * <p>Zones are defined as percentages of the user's max heart rate: zone zero is below 50%, and
 * each subsequent zone spans 10 percentage points up to zone five at 90–100%.
 *
 * @param zoneZeroMilli  time below 50% of max heart rate
 * @param zoneOneMilli   time at 50–60% of max heart rate
 * @param zoneTwoMilli   time at 60–70% of max heart rate
 * @param zoneThreeMilli time at 70–80% of max heart rate
 * @param zoneFourMilli  time at 80–90% of max heart rate
 * @param zoneFiveMilli  time at 90–100% of max heart rate
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ZoneDurations(
        long zoneZeroMilli,
        long zoneOneMilli,
        long zoneTwoMilli,
        long zoneThreeMilli,
        long zoneFourMilli,
        long zoneFiveMilli) {

    /** Zone durations indexed 0–5. */
    @JsonIgnore
    public List<Duration> asList() {
        return List.of(
                Duration.ofMillis(zoneZeroMilli),
                Duration.ofMillis(zoneOneMilli),
                Duration.ofMillis(zoneTwoMilli),
                Duration.ofMillis(zoneThreeMilli),
                Duration.ofMillis(zoneFourMilli),
                Duration.ofMillis(zoneFiveMilli));
    }

    /** Time spent in zones three through five, the high-intensity range. */
    @JsonIgnore
    public Duration highIntensity() {
        return Duration.ofMillis(zoneThreeMilli + zoneFourMilli + zoneFiveMilli);
    }
}
