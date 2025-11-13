package com.shredforge.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Parameters for a practice session the repository should orchestrate.
 */
public record SessionRequest(String userId, TabData tabData, CalibrationProfile calibrationProfile, Instant startedAt) {

    public SessionRequest {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tabData, "tabData");
        Objects.requireNonNull(calibrationProfile, "calibrationProfile");
        startedAt = startedAt == null ? Instant.now() : startedAt;
    }
}
