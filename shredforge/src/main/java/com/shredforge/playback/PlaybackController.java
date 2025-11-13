package com.shredforge.playback;

import com.shredforge.model.Tab;
import com.shredforge.model.Note;
import java.util.logging.Logger;

/**
 * Controls playback of guitar tabs during practice sessions.
 * Manages playback speed, position, looping, and timing.
 * Per specification section 3.3.3
 */
public class PlaybackController {
    private static final Logger LOGGER = Logger.getLogger(PlaybackController.class.getName());

    private Tab currentTab;
    private float playbackSpeed;  // 0.5 to 1.0 (50% to 100%)
    private boolean isPlaying;
    private int currentPosition;  // Current note index
    private int loopStart;
    private int loopEnd;

    private long playbackStartTime;
    private long pausedTime;
    private boolean isPaused;

    public PlaybackController() {
        this.playbackSpeed = 1.0f;
        this.isPlaying = false;
        this.currentPosition = 0;
        this.loopStart = -1;
        this.loopEnd = -1;
        this.isPaused = false;
    }

    /**
     * Start playback
     */
    public void start() {
        if (currentTab == null) {
            LOGGER.warning("Cannot start playback: no tab loaded");
            return;
        }

        isPlaying = true;
        isPaused = false;
        playbackStartTime = System.currentTimeMillis();
        currentPosition = 0;

        LOGGER.info("Playback started: " + currentTab.getTitle());
    }

    /**
     * Pause playback
     */
    public void pause() {
        if (!isPlaying) return;

        isPaused = true;
        pausedTime = System.currentTimeMillis();
        LOGGER.info("Playback paused");
    }

    /**
     * Resume playback
     */
    public void resume() {
        if (!isPaused) return;

        isPaused = false;
        long pauseDuration = System.currentTimeMillis() - pausedTime;
        playbackStartTime += pauseDuration;
        LOGGER.info("Playback resumed");
    }

    /**
     * Stop playback
     */
    public void stop() {
        isPlaying = false;
        isPaused = false;
        currentPosition = 0;
        LOGGER.info("Playback stopped");
    }

    /**
     * Set playback speed (0.5 to 1.0)
     */
    public void setSpeed(float speed) {
        if (speed < 0.5f || speed > 1.0f) {
            LOGGER.warning("Invalid playback speed: " + speed);
            return;
        }

        this.playbackSpeed = speed;
        LOGGER.info("Playback speed set to: " + (speed * 100) + "%");
    }

    /**
     * Set loop section (measure numbers)
     */
    public void setLoopSection(int start, int end) {
        if (start < 0 || end < start) {
            LOGGER.warning("Invalid loop section");
            return;
        }

        this.loopStart = start;
        this.loopEnd = end;
        LOGGER.info("Loop section set: " + start + " to " + end);
    }

    /**
     * Clear loop section
     */
    public void clearLoop() {
        this.loopStart = -1;
        this.loopEnd = -1;
        LOGGER.info("Loop cleared");
    }

    /**
     * Get current playback position in milliseconds
     */
    public int getCurrentPosition() {
        if (!isPlaying || isPaused) {
            return currentPosition;
        }

        long elapsed = System.currentTimeMillis() - playbackStartTime;
        return (int) (elapsed * playbackSpeed);
    }

    /**
     * Get expected note at current time
     */
    public Note getCurrentNote() {
        if (currentTab == null || !isPlaying) {
            return null;
        }

        int currentTime = getCurrentPosition();

        // Find note at current time with tolerance
        for (int i = currentPosition; i < currentTab.getNotes().size(); i++) {
            Note note = currentTab.getNotes().get(i);
            long adjustedTime = (long) (note.getTimestamp() / playbackSpeed);

            if (adjustedTime >= currentTime - 100 && adjustedTime <= currentTime + 100) {
                currentPosition = i;
                return note;
            }
        }

        return null;
    }

    /**
     * Get next expected note
     */
    public Note getNextNote() {
        if (currentTab == null || currentPosition >= currentTab.getNotes().size() - 1) {
            return null;
        }

        return currentTab.getNotes().get(currentPosition + 1);
    }

    /**
     * Check if playback is complete
     */
    public boolean isComplete() {
        if (currentTab == null || !isPlaying) {
            return false;
        }

        return currentPosition >= currentTab.getNotes().size() - 1;
    }

    /**
     * Get progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (currentTab == null || currentTab.getNotes().isEmpty()) {
            return 0;
        }

        return (currentPosition * 100) / currentTab.getNotes().size();
    }

    /**
     * Load tab for playback
     */
    public void loadTab(Tab tab) {
        this.currentTab = tab;
        this.currentPosition = 0;
        LOGGER.info("Tab loaded for playback: " + tab.getTitle());
    }

    // Getters
    public Tab getCurrentTab() { return currentTab; }
    public float getPlaybackSpeed() { return playbackSpeed; }
    public boolean isPlaying() { return isPlaying && !isPaused; }
    public boolean isPaused() { return isPaused; }
    public int getLoopStart() { return loopStart; }
    public int getLoopEnd() { return loopEnd; }
}
