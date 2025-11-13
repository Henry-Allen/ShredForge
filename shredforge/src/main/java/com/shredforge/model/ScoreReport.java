package com.shredforge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed performance report generated after a practice session.
 * Displays accuracy, timing, problem areas, and grade.
 */
public class ScoreReport {
    private final Session session;
    private final float accuracyPercentage;
    private final String grade;  // S, A, B, C, D
    private final int starRating;  // 1-5
    private final float averageTimingError;
    private final List<Integer> problemMeasures;

    public ScoreReport(Session session) {
        this.session = session;
        this.accuracyPercentage = session.getAccuracyPercentage();
        this.grade = calculateGrade(accuracyPercentage);
        this.starRating = calculateStarRating(accuracyPercentage);
        this.averageTimingError = session.getAverageTimingError();
        this.problemMeasures = new ArrayList<>();
    }

    /**
     * Calculate letter grade based on accuracy
     */
    private String calculateGrade(float accuracy) {
        if (accuracy >= 95.0f) return "S";
        if (accuracy >= 85.0f) return "A";
        if (accuracy >= 75.0f) return "B";
        if (accuracy >= 65.0f) return "C";
        return "D";
    }

    /**
     * Calculate star rating (1-5) based on accuracy
     */
    private int calculateStarRating(float accuracy) {
        if (accuracy >= 95.0f) return 5;
        if (accuracy >= 85.0f) return 4;
        if (accuracy >= 75.0f) return 3;
        if (accuracy >= 65.0f) return 2;
        return 1;
    }

    /**
     * Identify measures with low accuracy (problem areas)
     */
    public void analyzeProblemAreas() {
        // This would analyze session data to find specific measures with low accuracy
        // For now, this is a placeholder
        problemMeasures.clear();
    }

    /**
     * Get formatted duration string (MM:SS)
     */
    public String getFormattedDuration() {
        long durationMs = session.getDuration();
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get summary statistics as formatted string
     */
    public String getSummary() {
        return String.format(
            "Grade: %s (%d stars)\n" +
            "Accuracy: %.1f%%\n" +
            "Correct Notes: %d/%d\n" +
            "Incorrect: %d, Missed: %d\n" +
            "Avg Timing Error: %.0fms\n" +
            "Longest Streak: %d\n" +
            "Duration: %s",
            grade, starRating,
            accuracyPercentage,
            session.getCorrectNotes(), session.getTotalNotes(),
            session.getIncorrectNotes(), session.getMissedNotes(),
            averageTimingError,
            session.getLongestStreak(),
            getFormattedDuration()
        );
    }

    // Getters
    public Session getSession() { return session; }
    public float getAccuracyPercentage() { return accuracyPercentage; }
    public String getGrade() { return grade; }
    public int getStarRating() { return starRating; }
    public float getAverageTimingError() { return averageTimingError; }
    public List<Integer> getProblemMeasures() { return problemMeasures; }

    @Override
    public String toString() {
        return getSummary();
    }
}
