package com.shredforge.playback;

import com.shredforge.model.ScoreReport;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Calculates and tracks performance metrics during practice sessions.
 * Per specification section 3.3.3
 */
public class ScoreCalculator {
    private static final Logger LOGGER = Logger.getLogger(ScoreCalculator.class.getName());

    private int totalNotes;
    private int correctNotes;
    private int incorrectNotes;
    private int missedNotes;

    private final List<Float> timingErrors;
    private float accuracyPercentage;

    public ScoreCalculator() {
        this.totalNotes = 0;
        this.correctNotes = 0;
        this.incorrectNotes = 0;
        this.missedNotes = 0;
        this.timingErrors = new ArrayList<>();
        this.accuracyPercentage = 0.0f;
    }

    /**
     * Record a correct note
     */
    public void recordCorrectNote() {
        correctNotes++;
        updateAccuracy();
        LOGGER.fine("Correct note recorded. Total: " + correctNotes);
    }

    /**
     * Record an incorrect note
     */
    public void recordIncorrectNote() {
        incorrectNotes++;
        updateAccuracy();
        LOGGER.fine("Incorrect note recorded. Total: " + incorrectNotes);
    }

    /**
     * Record a missed note
     */
    public void recordMissedNote() {
        missedNotes++;
        updateAccuracy();
        LOGGER.fine("Missed note recorded. Total: " + missedNotes);
    }

    /**
     * Record timing error
     * @param error Timing error in milliseconds
     */
    public void recordTimingError(float error) {
        timingErrors.add(error);
        LOGGER.fine("Timing error recorded: " + error + "ms");
    }

    /**
     * Calculate overall score (0-100)
     */
    public float calculateScore() {
        if (totalNotes == 0) return 0.0f;

        // Base score from accuracy
        float baseScore = accuracyPercentage;

        // Penalty for timing errors
        float avgTiming = getAverageTimingError();
        float timingPenalty = 0;

        if (avgTiming > 50) {  // More than 50ms average error
            timingPenalty = (avgTiming - 50) * 0.1f;  // 0.1% penalty per ms over 50ms
        }

        float finalScore = Math.max(0, baseScore - timingPenalty);

        LOGGER.info("Score calculated: " + finalScore + "% (base: " + baseScore +
                    "%, timing penalty: " + timingPenalty + "%)");

        return finalScore;
    }

    /**
     * Update accuracy percentage
     */
    private void updateAccuracy() {
        int attemptedNotes = correctNotes + incorrectNotes;
        if (attemptedNotes == 0) {
            accuracyPercentage = 0.0f;
            return;
        }

        accuracyPercentage = ((float) correctNotes / attemptedNotes) * 100.0f;
    }

    /**
     * Get average timing error
     */
    public float getAverageTimingError() {
        if (timingErrors.isEmpty()) return 0.0f;

        float sum = 0;
        for (Float error : timingErrors) {
            sum += Math.abs(error);
        }

        return sum / timingErrors.size();
    }

    /**
     * Reset all counters
     */
    public void reset() {
        totalNotes = 0;
        correctNotes = 0;
        incorrectNotes = 0;
        missedNotes = 0;
        timingErrors.clear();
        accuracyPercentage = 0.0f;

        LOGGER.info("Score calculator reset");
    }

    /**
     * Set total notes for the session
     */
    public void setTotalNotes(int total) {
        this.totalNotes = total;
        LOGGER.info("Total notes set to: " + total);
    }

    // Getters
    public int getTotalNotes() { return totalNotes; }
    public int getCorrectNotes() { return correctNotes; }
    public int getIncorrectNotes() { return incorrectNotes; }
    public int getMissedNotes() { return missedNotes; }
    public float getAccuracyPercentage() { return accuracyPercentage; }
    public List<Float> getTimingErrors() { return new ArrayList<>(timingErrors); }

    /**
     * Get summary statistics
     */
    public String getSummary() {
        return String.format(
            "Performance Summary:\n" +
            "Total Notes: %d\n" +
            "Correct: %d (%.1f%%)\n" +
            "Incorrect: %d\n" +
            "Missed: %d\n" +
            "Accuracy: %.1f%%\n" +
            "Avg Timing Error: %.1fms\n" +
            "Final Score: %.1f",
            totalNotes,
            correctNotes, (totalNotes > 0 ? (correctNotes * 100.0f / totalNotes) : 0),
            incorrectNotes,
            missedNotes,
            accuracyPercentage,
            getAverageTimingError(),
            calculateScore()
        );
    }
}
