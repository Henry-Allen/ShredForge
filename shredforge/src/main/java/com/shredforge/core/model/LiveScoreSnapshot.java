package com.shredforge.core.model;

import java.util.List;

/**
 * Real-time scoring snapshot that provides both overall and partial (so-far) accuracy.
 * Updated continuously during a practice session.
 */
public record LiveScoreSnapshot(
        int totalNotesInSong,
        int notesPlayedSoFar,
        int hitsOverall,
        int missesOverall,
        int hitsSoFar,
        int missesSoFar,
        double currentPositionMs,
        double totalDurationMs,
        List<String> recentFeedback) {

    public LiveScoreSnapshot {
        recentFeedback = recentFeedback == null ? List.of() : List.copyOf(recentFeedback);
    }

    /**
     * Overall accuracy: hits / total notes in song.
     * This is the "final score" percentage if the user played the whole song.
     */
    public double overallAccuracyPercent() {
        if (totalNotesInSong == 0) {
            return 0.0;
        }
        return (hitsOverall * 100.0) / totalNotesInSong;
    }

    /**
     * Partial accuracy: hits / notes encountered so far.
     * This shows how well the user is doing on the portion they've played.
     */
    public double partialAccuracyPercent() {
        if (notesPlayedSoFar == 0) {
            return 0.0;
        }
        return (hitsSoFar * 100.0) / notesPlayedSoFar;
    }

    /**
     * Progress through the song as a percentage.
     */
    public double progressPercent() {
        if (totalDurationMs <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (currentPositionMs * 100.0) / totalDurationMs);
    }

    /**
     * Creates an empty snapshot for initialization.
     */
    public static LiveScoreSnapshot empty() {
        return new LiveScoreSnapshot(0, 0, 0, 0, 0, 0, 0, 0, List.of());
    }

    /**
     * Creates a snapshot with the given totals but no progress yet.
     */
    public static LiveScoreSnapshot initial(int totalNotes, double totalDurationMs) {
        return new LiveScoreSnapshot(totalNotes, 0, 0, 0, 0, 0, 0, totalDurationMs, List.of());
    }
}
