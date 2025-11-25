package com.shredforge.scoring;

import com.shredforge.core.model.AudioDeviceInfo;
import com.shredforge.core.model.ExpectedNote;
import com.shredforge.core.model.LiveScoreSnapshot;
import com.shredforge.core.model.PracticeConfig;
import com.shredforge.core.ports.PracticeScoringService;
import com.shredforge.scoring.detection.CqtNoteDetectionEngine;
import com.shredforge.scoring.detection.NoteDetectionListener;
import com.shredforge.scoring.model.DetectedNote;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Real-time practice scoring service that syncs with AlphaTab playback.
 * Matches detected notes against expected notes and provides live scoring.
 */
public final class LivePracticeScoringService implements PracticeScoringService, NoteDetectionListener {

    private final CqtNoteDetectionEngine detectionEngine;
    private final Object lock = new Object();

    // Session state
    private List<ExpectedNote> expectedNotes = List.of();
    private double totalDurationMs = 0;
    private volatile double currentPositionMs = 0;
    private volatile boolean sessionActive = false;
    private volatile boolean sessionPaused = false;
    private PracticeConfig currentConfig;
    private Consumer<LiveScoreSnapshot> snapshotListener;
    private NoteResultListener noteResultListener;

    /**
     * Listener for note hit/miss events to enable visual feedback.
     */
    public interface NoteResultListener {
        void onNoteHit(ExpectedNote note, int noteIndex);
        void onNoteMissed(ExpectedNote note, int noteIndex);
    }

    // Scoring state
    private final Set<Integer> hitNoteIndices = ConcurrentHashMap.newKeySet();
    private final Set<Integer> missedNoteIndices = ConcurrentHashMap.newKeySet();
    private final Set<Integer> processedNoteIndices = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedDeque<String> recentFeedback = new ConcurrentLinkedDeque<>();
    private static final int MAX_FEEDBACK_ITEMS = 5;

    // Detection buffer for matching
    private final ConcurrentLinkedQueue<DetectedNote> detectionBuffer = new ConcurrentLinkedQueue<>();
    private static final int MAX_BUFFER_SIZE = 100;

    // Snapshot update scheduling
    private ScheduledExecutorService scheduler;
    private static final long SNAPSHOT_INTERVAL_MS = 100;

    public LivePracticeScoringService() {
        this.detectionEngine = new CqtNoteDetectionEngine();
    }

    public LivePracticeScoringService(CqtNoteDetectionEngine engine) {
        this.detectionEngine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public List<AudioDeviceInfo> listAudioDevices() {
        return CqtNoteDetectionEngine.listInputDevices();
    }

    @Override
    public void loadExpectedNotes(List<ExpectedNote> notes, double totalDurationMs) {
        synchronized (lock) {
            if (notes == null || notes.isEmpty()) {
                this.expectedNotes = List.of();
            } else {
                // Sort notes by time for efficient matching
                List<ExpectedNote> sorted = new ArrayList<>(notes);
                sorted.sort(Comparator.comparingDouble(ExpectedNote::timeMs));
                this.expectedNotes = List.copyOf(sorted);
            }
            this.totalDurationMs = totalDurationMs;
            resetScoringState();
            System.out.println("Loaded " + this.expectedNotes.size() + " expected notes, duration: " + totalDurationMs + "ms");
            
            // Debug: print first few notes to verify timing
            if (!this.expectedNotes.isEmpty()) {
                System.out.println("First note at: " + this.expectedNotes.get(0).timeMs() + "ms");
                if (this.expectedNotes.size() > 1) {
                    System.out.println("Second note at: " + this.expectedNotes.get(1).timeMs() + "ms");
                }
            }
        }
    }

    /**
     * Sets the listener for note hit/miss events (for visual feedback).
     */
    public void setNoteResultListener(NoteResultListener listener) {
        this.noteResultListener = listener;
    }

    @Override
    public void startSession(PracticeConfig config, Consumer<LiveScoreSnapshot> snapshotListener) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(snapshotListener, "snapshotListener");

        synchronized (lock) {
            if (sessionActive) {
                stopSession();
            }

            this.currentConfig = config;
            this.snapshotListener = snapshotListener;
            this.sessionActive = true;
            this.sessionPaused = false;
            resetScoringState();

            // Start detection engine
            detectionEngine.setAudioDevice(config.audioDevice());
            detectionEngine.start(config, this);

            // Start snapshot update scheduler
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "score-snapshot-updater");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(this::processAndNotify, 
                    SNAPSHOT_INTERVAL_MS, SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);

            System.out.println("Practice session started with device: " + config.audioDevice().name());
        }
    }

    @Override
    public void updatePlaybackPosition(double positionMs) {
        if (!sessionActive || sessionPaused) {
            return;
        }

        double adjustedPosition = positionMs - (currentConfig != null ? currentConfig.latencyCompensationMs() : 0);
        double oldPosition = this.currentPositionMs;
        this.currentPositionMs = Math.max(0, adjustedPosition);

        // Debug: Log position updates periodically (every ~500ms worth of movement)
        if (Math.floor(currentPositionMs / 500) != Math.floor(oldPosition / 500)) {
            double tolerance = currentConfig != null ? currentConfig.timingToleranceMs() : 500;
            int notesInWindow = countNotesInWindow(currentPositionMs, tolerance);
            System.out.println(String.format("[POSITION] %.0fms - %d notes in window [%.0f - %.0f]ms",
                    currentPositionMs, notesInWindow, currentPositionMs - tolerance, currentPositionMs + tolerance));
        }

        // Process notes that have passed
        processPassedNotes();
    }

    private int countNotesInWindow(double position, double tolerance) {
        int count = 0;
        for (int i = 0; i < expectedNotes.size(); i++) {
            if (processedNoteIndices.contains(i)) continue;
            double noteTime = expectedNotes.get(i).timeMs();
            if (noteTime >= position - tolerance && noteTime <= position + tolerance) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void pauseSession() {
        synchronized (lock) {
            if (sessionActive) {
                sessionPaused = true;
                detectionEngine.stop();
            }
        }
    }

    @Override
    public void resumeSession() {
        synchronized (lock) {
            if (sessionActive && sessionPaused && currentConfig != null) {
                sessionPaused = false;
                detectionEngine.start(currentConfig, this);
            }
        }
    }

    @Override
    public LiveScoreSnapshot stopSession() {
        synchronized (lock) {
            sessionActive = false;
            sessionPaused = false;

            detectionEngine.stop();

            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }

            LiveScoreSnapshot finalSnapshot = buildSnapshot();
            System.out.println("Practice session stopped. Final accuracy: " + 
                    String.format("%.1f%%", finalSnapshot.partialAccuracyPercent()));
            return finalSnapshot;
        }
    }

    @Override
    public LiveScoreSnapshot getCurrentSnapshot() {
        return buildSnapshot();
    }

    @Override
    public boolean isSessionActive() {
        return sessionActive && !sessionPaused;
    }

    @Override
    public void resetSession() {
        synchronized (lock) {
            resetScoringState();
            currentPositionMs = 0;
        }
    }

    @Override
    public void close() {
        stopSession();
        detectionEngine.stop();
    }

    // NoteDetectionListener implementation
    @Override
    public void onNoteDetected(DetectedNote note) {
        if (!sessionActive || sessionPaused || note == null) {
            return;
        }

        // DEBUG: Log every detected note
        System.out.println(String.format("[DETECT] Note: %s (MIDI %.1f, %.1f Hz, conf=%.2f) @ pos=%.0fms",
                note.noteName(), note.midi(), note.frequencyHz(), note.confidence(), currentPositionMs));

        // Add to buffer, removing old entries if needed
        detectionBuffer.offer(note);
        while (detectionBuffer.size() > MAX_BUFFER_SIZE) {
            detectionBuffer.poll();
        }

        // Try to match immediately
        tryMatchNote(note);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Detection error: " + throwable.getMessage());
        addFeedback("Detection error: " + throwable.getMessage());
    }

    // Private methods

    private void resetScoringState() {
        hitNoteIndices.clear();
        missedNoteIndices.clear();
        processedNoteIndices.clear();
        detectionBuffer.clear();
        recentFeedback.clear();
    }

    private void tryMatchNote(DetectedNote detected) {
        if (currentConfig == null || expectedNotes.isEmpty()) {
            return;
        }

        double tolerance = currentConfig.timingToleranceMs();
        double pitchTolerance = currentConfig.pitchToleranceCents();

        // Find expected notes within the timing window around current playback position
        // Only consider notes that playback has reached (or is about to reach)
        int notesInWindow = 0;
        for (int i = 0; i < expectedNotes.size(); i++) {
            if (hitNoteIndices.contains(i) || missedNoteIndices.contains(i)) {
                continue;
            }

            ExpectedNote expected = expectedNotes.get(i);
            double noteTime = expected.timeMs();
            
            // Only match notes that are within the timing window of current playback
            // Note must be: (currentPosition - tolerance) <= noteTime <= (currentPosition + tolerance)
            // This prevents matching notes far in the future or past
            if (noteTime < currentPositionMs - tolerance) {
                // Note is too far in the past - skip (will be marked as missed by processPassedNotes)
                continue;
            }
            if (noteTime > currentPositionMs + tolerance) {
                // Note is too far in the future - stop searching (notes are sorted by time)
                break;
            }

            notesInWindow++;
            
            // Check pitch match
            double expectedMidi = expected.midi();
            double detectedMidi = detected.midi();
            double centsDiff = Math.abs((detectedMidi - expectedMidi) * 100);

            // DEBUG: Log comparison
            System.out.println(String.format("  [COMPARE] Expected: %s (MIDI %d) @ %.0fms vs Detected: %s (MIDI %.1f) - diff=%.0f cents (tol=%.0f)",
                    expected.noteName(), expected.midi(), noteTime, detected.noteName(), detectedMidi, centsDiff, pitchTolerance));

            if (centsDiff <= pitchTolerance) {
                // Hit!
                hitNoteIndices.add(i);
                processedNoteIndices.add(i);
                System.out.println(String.format("  [HIT] Matched note %d: %s", i, expected.noteName()));
                addFeedback("✓ " + expected.noteName() + " (+" + String.format("%.0f", centsDiff) + "¢)");
                // Notify listener for visual feedback
                if (noteResultListener != null) {
                    noteResultListener.onNoteHit(expected, i);
                }
                return; // Only match one note per detection
            }
        }
        
        if (notesInWindow == 0) {
            System.out.println(String.format("  [NO NOTES] No expected notes in window [%.0f - %.0f]ms", 
                    currentPositionMs - tolerance, currentPositionMs + tolerance));
        }
    }

    private void processPassedNotes() {
        if (currentConfig == null || expectedNotes.isEmpty()) {
            return;
        }

        double tolerance = currentConfig.timingToleranceMs();

        for (int i = 0; i < expectedNotes.size(); i++) {
            if (processedNoteIndices.contains(i)) {
                continue;
            }

            ExpectedNote expected = expectedNotes.get(i);
            double noteEndTime = expected.timeMs() + tolerance; // Use note start + tolerance as the deadline

            // If we've passed this note's window and it wasn't hit, mark as missed
            if (currentPositionMs > noteEndTime) {
                if (!hitNoteIndices.contains(i)) {
                    missedNoteIndices.add(i);
                    System.out.println(String.format("[MISSED] Note %d: %s @ %.0fms (pos=%.0fms)",
                            i, expected.noteName(), expected.timeMs(), currentPositionMs));
                    // Notify listener for visual feedback
                    if (noteResultListener != null) {
                        noteResultListener.onNoteMissed(expected, i);
                    }
                    addFeedback("✗ Missed " + expected.noteName());
                }
                processedNoteIndices.add(i);
            }
        }
    }

    private void processAndNotify() {
        if (!sessionActive || sessionPaused || snapshotListener == null) {
            return;
        }

        try {
            LiveScoreSnapshot snapshot = buildSnapshot();
            snapshotListener.accept(snapshot);
        } catch (Exception e) {
            System.err.println("Error updating snapshot: " + e.getMessage());
        }
    }

    private LiveScoreSnapshot buildSnapshot() {
        int totalNotes = expectedNotes.size();
        int hits = hitNoteIndices.size();
        int misses = missedNoteIndices.size();
        int processed = processedNoteIndices.size();

        // For partial scoring, only count notes we've encountered
        int hitsSoFar = hits;
        int missesSoFar = misses;

        List<String> feedback = new ArrayList<>(recentFeedback);

        return new LiveScoreSnapshot(
                totalNotes,
                processed,
                hits,
                misses,
                hitsSoFar,
                missesSoFar,
                currentPositionMs,
                totalDurationMs,
                feedback
        );
    }

    private void addFeedback(String message) {
        recentFeedback.addFirst(message);
        while (recentFeedback.size() > MAX_FEEDBACK_ITEMS) {
            recentFeedback.removeLast();
        }
    }
}
