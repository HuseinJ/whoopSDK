package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Strain and cardiovascular load accumulated over a {@link Cycle}.
 *
 * @param strain           WHOOP strain on a 0–21 scale
 * @param kilojoule        energy expenditure in kilojoules
 * @param averageHeartRate average bpm across the cycle
 * @param maxHeartRate     peak bpm across the cycle
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CycleScore(
        float strain,
        float kilojoule,
        int averageHeartRate,
        int maxHeartRate) {

    /** Energy expenditure converted to calories (kcal). */
    @JsonIgnore
    public double calories() {
        return kilojoule / 4.184d;
    }
}
