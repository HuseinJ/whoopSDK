package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;

/**
 * The recovery metrics behind the colour-coded score in the WHOOP app.
 *
 * @param userCalibrating   {@code true} while WHOOP is still establishing the user's baseline; the
 *                          score is less meaningful during this period
 * @param recoveryScore     0–100 percent
 * @param restingHeartRate  resting bpm
 * @param hrvRmssdMilli     heart-rate variability (RMSSD) in milliseconds
 * @param spo2Percentage    blood-oxygen saturation, when measured
 * @param skinTempCelsius   skin temperature, when measured
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecoveryScore(
        boolean userCalibrating,
        float recoveryScore,
        float restingHeartRate,
        float hrvRmssdMilli,
        Float spo2Percentage,
        Float skinTempCelsius) {

    @JsonIgnore
    public Optional<Float> spo2() {
        return Optional.ofNullable(spo2Percentage);
    }

    @JsonIgnore
    public Optional<Float> skinTemp() {
        return Optional.ofNullable(skinTempCelsius);
    }
}
