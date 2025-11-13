package com.shredforge.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Persisted calibration data for a specific user/guitar setup.
 */
public record CalibrationProfile(
        String userId,
        double inputGain,
        double noiseFloorDb,
        Map<String, Double> stringOffsetsCents,
        Instant createdAt) {

    public CalibrationProfile {
        Objects.requireNonNull(userId, "userId");
        stringOffsetsCents = stringOffsetsCents == null ? Map.of() : Map.copyOf(stringOffsetsCents);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
