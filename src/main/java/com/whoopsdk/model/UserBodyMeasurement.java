package com.whoopsdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The authorized user's body metrics. Requires the {@code read:body_measurement} scope.
 *
 * @param heightMeter    height in metres
 * @param weightKilogram weight in kilograms
 * @param maxHeartRate   the max heart rate WHOOP uses to compute training zones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserBodyMeasurement(
        float heightMeter,
        float weightKilogram,
        int maxHeartRate) {
}
