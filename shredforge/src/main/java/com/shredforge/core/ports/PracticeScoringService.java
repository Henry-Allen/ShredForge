package com.shredforge.core.ports;

import com.shredforge.core.model.AudioDeviceInfo;
import com.shredforge.core.model.ExpectedNote;
import com.shredforge.core.model.LiveScoreSnapshot;
import com.shredforge.core.model.PracticeConfig;

import java.util.List;
import java.util.function.Consumer;

/**
 * Port for real-time practice scoring that syncs with AlphaTab playback.
 * Implementations handle audio detection, note matching, and score calculation.
 */
public interface PracticeScoringService extends AutoCloseable {

    /**
     * Lists available audio input devices.
     */
    List<AudioDeviceInfo> listAudioDevices();

    /**
     * Loads the expected notes for the current song.
     * Called when AlphaTab finishes loading a score.
     *
     * @param notes list of expected notes extracted from AlphaTab
     * @param totalDurationMs total duration of the song in milliseconds
     */
    void loadExpectedNotes(List<ExpectedNote> notes, double totalDurationMs);

    /**
     * Starts a practice session with the given configuration.
     * Begins audio detection and score tracking.
     *
     * @param config practice configuration including audio device and tolerances
     * @param snapshotListener callback invoked periodically with score updates
     */
    void startSession(PracticeConfig config, Consumer<LiveScoreSnapshot> snapshotListener);

    /**
     * Updates the current playback position from AlphaTab.
     * Called frequently during playback to sync scoring with the tab.
     *
     * @param positionMs current playback position in milliseconds
     */
    void updatePlaybackPosition(double positionMs);

    /**
     * Pauses the practice session (stops scoring but retains state).
     */
    void pauseSession();

    /**
     * Resumes a paused practice session.
     */
    void resumeSession();

    /**
     * Stops the practice session and returns the final score snapshot.
     */
    LiveScoreSnapshot stopSession();

    /**
     * Returns the current score snapshot without stopping the session.
     */
    LiveScoreSnapshot getCurrentSnapshot();

    /**
     * Returns true if a session is currently active.
     */
    boolean isSessionActive();

    /**
     * Resets the session state for a new attempt without reloading notes.
     */
    void resetSession();

    @Override
    void close();
}
