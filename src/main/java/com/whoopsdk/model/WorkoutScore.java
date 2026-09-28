package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

/**
 * Scoring for a {@link Workout}.
 *
 * @param strain             WHOOP strain on a 0–21 scale
 * @param averageHeartRate   average bpm
 * @param maxHeartRate       peak bpm
 * @param kilojoule          energy expenditure in kilojoules
 * @param percentRecorded    share of the workout the strap actually captured, 0–100
 * @param distanceMeter      distance covered, when the activity provides it
 * @param altitudeGainMeter  cumulative ascent, when available
 * @param altitudeChangeMeter net altitude change, when available
 * @param zoneDurations      time spent in each heart-rate zone
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkoutScore(
        float strain,
        int averageHeartRate,
        int maxHeartRate,
        float kilojoule,
        float percentRecorded,
        Float distanceMeter,
        Float altitudeGainMeter,
        Float altitudeChangeMeter,
        ZoneDurations zoneDurations) {

    /** Energy expenditure converted to calories (kcal). */
    @JsonIgnore
    public double calories() {
        return kilojoule / 4.184d;
    }

    @JsonIgnore
    public Optional<Float> distance() {
        return Optional.ofNullable(distanceMeter);
    }
}
