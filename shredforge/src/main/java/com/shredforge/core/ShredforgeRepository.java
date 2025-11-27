package com.shredforge.core;

import com.shredforge.core.model.AudioDeviceInfo;
import com.shredforge.calibration.GuitarTunerService;
import com.shredforge.calibration.TuningLibrary;
import com.shredforge.calibration.TuningPreset;
import com.shredforge.calibration.TuningSession;
import com.shredforge.core.model.ExpectedNote;
import com.shredforge.core.model.LiveScoreSnapshot;
import com.shredforge.core.model.PracticeConfig;
import com.shredforge.core.model.TabData;
import com.shredforge.core.ports.PracticeScoringService;
import com.shredforge.scoring.LivePracticeScoringService;
import com.shredforge.tabs.TabManager;
import com.shredforge.tabs.model.SongSelection;
import com.shredforge.tabs.model.TabSearchRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Primary entry point for UI/controllers. Exposes high-level operations the UI can
 * call without needing to understand the underlying subsystems.
 */
public final class ShredforgeRepository {

    private final TabManager tabManager;
    private final PracticeScoringService practiceScoringService;
    private final GuitarTunerService tunerService;

    private ShredforgeRepository(Builder builder) {
        this.tabManager = builder.tabManager != null ? builder.tabManager : TabManager.createDefault();
        this.practiceScoringService = builder.practiceScoringService != null 
                ? builder.practiceScoringService 
                : new LivePracticeScoringService();
        this.tunerService = new GuitarTunerService();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Searches for songs matching the given term.
     * Returns song-level results (each containing all tracks).
     */
    public List<SongSelection> searchSongs(String term) {
        return tabManager.searchSongs(new TabSearchRequest(term));
    }

    public TabManager tabManager() {
        return tabManager;
    }

    public List<TuningPreset> availableTunings() {
        return TuningLibrary.commonTunings();
    }

    /**
     * Downloads a GP file for the given song selection.
     * Returns a CompletableFuture that resolves to TabData with the GP file path.
     */
    public CompletableFuture<TabData> downloadGpFile(SongSelection selection) {
        return tabManager.downloadOrGetCached(selection);
    }

    // ==================== Practice Session API ====================

    /**
     * Lists available audio input devices for practice mode.
     */
    public List<AudioDeviceInfo> listAudioDevices() {
        return practiceScoringService.listAudioDevices();
    }

    /**
     * Loads expected notes extracted from AlphaTab for the current song.
     * Called when AlphaTab finishes loading a score.
     *
     * @param notes list of expected notes from the score
     * @param totalDurationMs total duration of the song in milliseconds
     */
    public void loadExpectedNotes(List<ExpectedNote> notes, double totalDurationMs) {
        practiceScoringService.loadExpectedNotes(notes, totalDurationMs);
    }

    /**
     * Starts a practice session with the given configuration.
     *
     * @param config practice configuration including audio device and tolerances
     * @param snapshotListener callback invoked periodically with score updates
     */
    public void startPracticeSession(PracticeConfig config, Consumer<LiveScoreSnapshot> snapshotListener) {
        practiceScoringService.startSession(config, snapshotListener);
    }

    /**
     * Sets a listener for note hit/miss events (for visual feedback in AlphaTab).
     * Must be called before starting a practice session.
     */
    public void setNoteResultListener(LivePracticeScoringService.NoteResultListener listener) {
        if (practiceScoringService instanceof LivePracticeScoringService liveService) {
            liveService.setNoteResultListener(listener);
        }
    }

    /**
     * Updates the current playback position from AlphaTab.
     * Called frequently during playback to sync scoring with the tab.
     *
     * @param positionMs current playback position in milliseconds
     */
    public void updatePlaybackPosition(double positionMs) {
        practiceScoringService.updatePlaybackPosition(positionMs);
    }

    /**
     * Pauses the practice session.
     */
    public void pausePracticeSession() {
        practiceScoringService.pauseSession();
    }

    /**
     * Resumes a paused practice session.
     */
    public void resumePracticeSession() {
        practiceScoringService.resumeSession();
    }

    /**
     * Stops the practice session and returns the final score snapshot.
     */
    public LiveScoreSnapshot stopPracticeSession() {
        return practiceScoringService.stopSession();
    }

    /**
     * Returns the current score snapshot without stopping the session.
     */
    public LiveScoreSnapshot getCurrentScore() {
        return practiceScoringService.getCurrentSnapshot();
    }

    /**
     * Returns true if a practice session is currently active.
     */
    public boolean isPracticeSessionActive() {
        return practiceScoringService.isSessionActive();
    }

    /**
     * Resets the practice session for a new attempt without reloading notes.
     */
    public void resetPracticeSession() {
        practiceScoringService.resetSession();
    }

    /**
     * Returns the default practice configuration.
     */
    public PracticeConfig defaultPracticeConfig() {
        return PracticeConfig.defaults();
    }

    // ==================== End Practice Session API ====================

    // ==================== Tuning Session API ====================

    /**
     * Creates a tuning session from MIDI note numbers (as extracted from AlphaTab).
     * @param tuningName display name for the tuning (e.g., "Standard", "Drop D")
     * @param midiNotes array of MIDI note numbers, one per string (high to low)
     * @return a new TuningSession
     */
    public TuningSession createTuningSession(String tuningName, int[] midiNotes) {
        return TuningSession.fromMidiNotes(tuningName, midiNotes);
    }

    /**
     * Creates a tuning session from a preset.
     * @param preset the tuning preset to use
     * @return a new TuningSession
     */
    public TuningSession createTuningSession(TuningPreset preset) {
        return TuningSession.fromPreset(preset);
    }

    /**
     * Creates a standard tuning session (EADGBE).
     * @return a new TuningSession for standard tuning
     */
    public TuningSession createStandardTuningSession() {
        return TuningSession.standardTuning();
    }

    /**
     * Starts a tuning session with real-time pitch detection.
     * @param session the tuning session to start
     * @param audioDevice the audio device to use for input
     * @param updateListener callback for real-time tuning updates
     */
    public void startTuning(TuningSession session, AudioDeviceInfo audioDevice, 
            java.util.function.Consumer<GuitarTunerService.TuningUpdate> updateListener) {
        tunerService.setAudioDevice(audioDevice);
        tunerService.startTuning(session, updateListener);
    }

    /**
     * Stops the current tuning session.
     */
    public void stopTuning() {
        tunerService.stopTuning();
    }

    /**
     * Returns true if a tuning session is currently active.
     */
    public boolean isTuningActive() {
        return tunerService.isRunning();
    }

    /**
     * Advances to the next string in the tuning session.
     * @return true if there are more strings to tune
     */
    public boolean tuningNextString() {
        return tunerService.nextString();
    }

    /**
     * Goes back to the previous string in the tuning session.
     * @return true if moved successfully
     */
    public boolean tuningPreviousString() {
        return tunerService.previousString();
    }

    /**
     * Confirms the current string is tuned and advances to the next.
     * @return true if there are more strings to tune
     */
    public boolean tuningConfirmAndAdvance() {
        return tunerService.confirmAndAdvance();
    }

    /**
     * Returns the current tuning session, or null if not tuning.
     */
    public TuningSession getCurrentTuningSession() {
        return tunerService.getSession();
    }

    // ==================== End Tuning Session API ====================

    public static final class Builder {
        private TabManager tabManager;
        private PracticeScoringService practiceScoringService;

        public Builder withTabManager(TabManager tabManager) {
            this.tabManager = Objects.requireNonNull(tabManager, "tabManager");
            return this;
        }

        public Builder withPracticeScoringService(PracticeScoringService service) {
            this.practiceScoringService = Objects.requireNonNull(service, "practiceScoringService");
            return this;
        }

        public ShredforgeRepository build() {
            return new ShredforgeRepository(this);
        }
    }
}
