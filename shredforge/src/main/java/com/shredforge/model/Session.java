package com.shredforge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a practice session for a specific tab.
 * Tracks performance metrics, timing, and note accuracy.
 */
public class Session {
    private final String sessionId;
    private final Tab tab;
    private final long startTime;
    private long endTime;

    private int totalNotes;
    private int correctNotes;
    private int incorrectNotes;
    private int missedNotes;

    private final List<Float> timingErrors;  // Millisecond offsets from expected timing
    private int currentStreak;
    private int longestStreak;

    private float playbackSpeed;  // 0.5 to 1.0 (50% to 100%)
    private int loopStart;  // Measure number
    private int loopEnd;    // Measure number

    private boolean isPaused;
    private boolean isCompleted;

    public Session(String sessionId, Tab tab) {
        this.sessionId = sessionId;
        this.tab = tab;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;

        this.totalNotes = tab.getTotalNotes();
        this.correctNotes = 0;
        this.incorrectNotes = 0;
        this.missedNotes = 0;

        this.timingErrors = new ArrayList<>();
        this.currentStreak = 0;
        this.longestStreak = 0;

        this.playbackSpeed = 1.0f;
        this.loopStart = -1;
        this.loopEnd = -1;

        this.isPaused = false;
        this.isCompleted = false;
    }

    /**
     * Record a correctly played note
     */
    public void recordCorrectNote(float timingOffset) {
        correctNotes++;
        currentStreak++;
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        timingErrors.add(timingOffset);
    }

    /**
     * Record an incorrectly played note
     */
    public void recordIncorrectNote() {
        incorrectNotes++;
        currentStreak = 0;
    }

    /**
     * Record a missed note (not played when expected)
     */
    public void recordMissedNote() {
        missedNotes++;
        currentStreak = 0;
    }

    /**
     * Calculate accuracy percentage
     */
    public float getAccuracyPercentage() {
        int attemptedNotes = correctNotes + incorrectNotes;
        if (attemptedNotes == 0) return 0.0f;
        return (float) correctNotes / attemptedNotes * 100.0f;
    }

    /**
     * Calculate average timing error in milliseconds
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
     * Get total practice duration in milliseconds
     */
    public long getDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * End the session
     */
    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.isCompleted = true;
    }

    /**
     * Generate a score report from this session
     */
    public ScoreReport generateReport() {
        return new ScoreReport(this);
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public Tab getTab() { return tab; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public int getTotalNotes() { return totalNotes; }
    public int getCorrectNotes() { return correctNotes; }
    public int getIncorrectNotes() { return incorrectNotes; }
    public int getMissedNotes() { return missedNotes; }

    public List<Float> getTimingErrors() { return timingErrors; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }

    public float getPlaybackSpeed() { return playbackSpeed; }
    public void setPlaybackSpeed(float speed) {
        if (speed < 0.5f || speed > 1.0f) {
            throw new IllegalArgumentException("Playback speed must be between 0.5 and 1.0");
        }
        this.playbackSpeed = speed;
    }

    public int getLoopStart() { return loopStart; }
    public void setLoopStart(int loopStart) { this.loopStart = loopStart; }

    public int getLoopEnd() { return loopEnd; }
    public void setLoopEnd(int loopEnd) { this.loopEnd = loopEnd; }

    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }

    public boolean isCompleted() { return isCompleted; }

    @Override
    public String toString() {
        return String.format("Session[%s, %s - %s, accuracy=%.1f%%, notes=%d/%d]",
                             sessionId, tab.getArtist(), tab.getTitle(),
                             getAccuracyPercentage(), correctNotes, totalNotes);
    }
}
