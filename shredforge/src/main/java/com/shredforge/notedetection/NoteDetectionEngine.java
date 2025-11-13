package com.shredforge.notedetection;

import com.shredforge.model.DetectedNote;
import com.shredforge.model.Note;
import com.shredforge.input.AudioInput;
import com.shredforge.input.GuitarSignalProcessor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Continuous note detection engine that processes audio input.
 * Compares detected notes with expected notes from tabs.
 */
public class NoteDetectionEngine {
    private static final Logger LOGGER = Logger.getLogger(NoteDetectionEngine.class.getName());
    private static final int TIMING_WINDOW_MS = 200;  // Timing tolerance

    private final AudioInput audioInput;
    private final GuitarSignalProcessor signalProcessor;
    private final FrequencyConversion frequencyConversion;

    private final Queue<DetectedNote> detectedNotes;
    private final Queue<Note> expectedNotes;

    private boolean isRunning;
    private Thread detectionThread;
    private float detectionThreshold;

    public NoteDetectionEngine() {
        this.audioInput = new AudioInput();
        this.signalProcessor = new GuitarSignalProcessor(
            audioInput.getSampleRate(),
            audioInput.getBufferSize()
        );
        this.frequencyConversion = new FrequencyConversion();

        this.detectedNotes = new ConcurrentLinkedQueue<>();
        this.expectedNotes = new ConcurrentLinkedQueue<>();

        this.isRunning = false;
        this.detectionThreshold = 0.6f;
    }

    /**
     * Start note detection
     */
    public boolean startDetection() {
        if (isRunning) {
            LOGGER.warning("Detection already running");
            return false;
        }

        if (!audioInput.openStream()) {
            LOGGER.severe("Failed to open audio stream");
            return false;
        }

        isRunning = true;

        detectionThread = new Thread(this::detectionLoop);
        detectionThread.setName("NoteDetectionThread");
        detectionThread.setDaemon(true);
        detectionThread.start();

        LOGGER.info("Note detection started");
        return true;
    }

    /**
     * Stop note detection
     */
    public void stopDetection() {
        isRunning = false;

        if (detectionThread != null) {
            try {
                detectionThread.join(1000);
            } catch (InterruptedException e) {
                LOGGER.warning("Interrupted while stopping detection");
            }
        }

        audioInput.closeStream();
        LOGGER.info("Note detection stopped");
    }

    /**
     * Main detection loop
     */
    private void detectionLoop() {
        LOGGER.info("Detection loop started");

        while (isRunning) {
            try {
                // Read audio data
                float[] audioData = audioInput.readAudioData();

                if (audioData.length > 0) {
                    // Process signal to detect frequency
                    float frequency = signalProcessor.processSignal(audioData);

                    if (frequency > 0) {
                        // Convert to note with confidence
                        float confidence = calculateConfidence(audioData);
                        DetectedNote note = signalProcessor.convertToNote(frequency, confidence);

                        if (note != null && note.getConfidence() >= detectionThreshold) {
                            detectedNotes.offer(note);
                            // Keep queue size manageable
                            while (detectedNotes.size() > 100) {
                                detectedNotes.poll();
                            }
                        }
                    }
                }

                // Small pause to prevent CPU overload
                Thread.sleep(10);

            } catch (InterruptedException e) {
                LOGGER.info("Detection loop interrupted");
                break;
            } catch (Exception e) {
                LOGGER.warning("Error in detection loop: " + e.getMessage());
            }
        }

        LOGGER.info("Detection loop ended");
    }

    /**
     * Calculate confidence based on signal quality
     */
    private float calculateConfidence(float[] audioData) {
        // Simple confidence based on signal amplitude and clarity
        float maxAmplitude = 0;
        float avgAmplitude = 0;

        for (float sample : audioData) {
            float abs = Math.abs(sample);
            maxAmplitude = Math.max(maxAmplitude, abs);
            avgAmplitude += abs;
        }

        avgAmplitude /= audioData.length;

        // Higher confidence if signal is strong and consistent
        float confidence = Math.min(1.0f, maxAmplitude * 2.0f);

        // Reduce confidence if signal is too noisy (high variance)
        if (avgAmplitude < maxAmplitude * 0.3f) {
            confidence *= 0.7f;
        }

        return confidence;
    }

    /**
     * Compare detected note with expected note
     */
    public boolean compareNotes(DetectedNote detected, Note expected) {
        if (detected == null || expected == null) {
            return false;
        }

        // Check if notes match
        if (!detected.matches(expected)) {
            return false;
        }

        // Check timing
        return calculateTiming(detected, expected) <= TIMING_WINDOW_MS;
    }

    /**
     * Calculate timing difference between detected and expected notes
     */
    public float calculateTiming(DetectedNote detected, Note expected) {
        return Math.abs(detected.getTimestamp() - expected.getTimestamp());
    }

    /**
     * Check if note is correct
     */
    public boolean isNoteCorrect(DetectedNote detected, Note expected) {
        return compareNotes(detected, expected);
    }

    /**
     * Get the most recent detected note
     */
    public DetectedNote getLastDetectedNote() {
        return detectedNotes.peek();
    }

    /**
     * Clear detected notes queue
     */
    public void clearDetectedNotes() {
        detectedNotes.clear();
    }

    /**
     * Set expected notes queue (from tab)
     */
    public void setExpectedNotes(Queue<Note> notes) {
        this.expectedNotes.clear();
        this.expectedNotes.addAll(notes);
    }

    // Getters and setters
    public boolean isRunning() {
        return isRunning;
    }

    public void setDetectionThreshold(float threshold) {
        this.detectionThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
    }

    public Queue<DetectedNote> getDetectedNotes() {
        return detectedNotes;
    }
}
