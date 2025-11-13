package com.shredforge.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Outcome of a single play-along session.
 */
public record SessionResult(
        String sessionId,
        double accuracyPercent,
        List<String> insights,
        Map<String, Double> stringBreakdown,
        Instant completedAt) {

    public SessionResult {
        insights = insights == null ? List.of() : List.copyOf(insights);
        stringBreakdown = stringBreakdown == null ? Map.of() : Map.copyOf(stringBreakdown);
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }
}
