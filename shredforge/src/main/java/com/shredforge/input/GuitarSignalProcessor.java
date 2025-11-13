package com.shredforge.input;

import com.shredforge.model.DetectedNote;
import com.shredforge.notedetection.FrequencyConversion;
import com.shredforge.util.AudioBufferPool;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes guitar audio signals to detect notes with optimized performance.
 * Uses autocorrelation-based pitch detection for accurate frequency analysis.
 */
public class GuitarSignalProcessor {
    private static final Logger LOGGER = Logger.getLogger(GuitarSignalProcessor.class.getName());
    private static final float MIN_FREQUENCY = 80.0f;   // Low E string (E2)
    private static final float MAX_FREQUENCY = 1320.0f; // High E at 24th fret
    private static final float CONFIDENCE_THRESHOLD = 0.6f;
    private static final float NOISE_GATE_THRESHOLD = 0.01f;

    private final float sampleRate;
    private final int bufferSize;
    private final FrequencyConversion frequencyConversion;
    private final AudioBufferPool bufferPool;

    // Cached values to reduce allocations
    private float lastDetectedFrequency;
    private long lastDetectionTime;
    private final float[] cachedDiffBuffer;
    private final int minPeriod;
    private final int maxPeriod;

    public GuitarSignalProcessor(float sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.frequencyConversion = new FrequencyConversion();
        this.bufferPool = new AudioBufferPool(bufferSize);
        this.lastDetectedFrequency = 0.0f;
        this.lastDetectionTime = 0;

        // Pre-calculate period ranges
        this.minPeriod = (int) (sampleRate / MAX_FREQUENCY);
        this.maxPeriod = (int) (sampleRate / MIN_FREQUENCY);

        // Pre-allocate diff buffer for autocorrelation
        this.cachedDiffBuffer = new float[maxPeriod];

        LOGGER.info("Signal processor initialized: " + sampleRate + "Hz, buffer=" + bufferSize);
    }

    /**
     * Process audio signal and detect pitch with error handling
     * @param audioBuffer Audio samples
     * @return Detected frequency in Hz, or -1 if no clear pitch detected
     */
    public float processSignal(float[] audioBuffer) {
        if (audioBuffer == null || audioBuffer.length == 0) {
            LOGGER.fine("Empty audio buffer provided");
            return -1.0f;
        }

        try {
            // Fast noise gate check with single pass
            float maxAmplitude = 0;
            for (int i = 0; i < audioBuffer.length; i++) {
                float abs = Math.abs(audioBuffer[i]);
                if (abs > maxAmplitude) {
                    maxAmplitude = abs;
                    // Early exit if we know it's above threshold
                    if (maxAmplitude > NOISE_GATE_THRESHOLD * 2) {
                        break;
                    }
                }
            }

            if (maxAmplitude < NOISE_GATE_THRESHOLD) {
                return -1.0f;
            }

            // Use optimized autocorrelation for pitch detection
            float frequency = detectPitchAutocorrelation(audioBuffer);

            // Filter out frequencies outside guitar range
            if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
                return -1.0f;
            }

            lastDetectedFrequency = frequency;
            lastDetectionTime = System.currentTimeMillis();

            return frequency;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing audio signal", e);
            return -1.0f;
        }
    }

    /**
     * Optimized autocorrelation-based pitch detection
     * Based on YIN algorithm principles with performance improvements
     */
    private float detectPitchAutocorrelation(float[] buffer) {
        try {
            float bestFreq = 0;
            float minDiff = Float.MAX_VALUE;

            // Use pre-calculated period ranges
            int searchMaxPeriod = Math.min(maxPeriod, buffer.length / 2);

            // Autocorrelation with optimizations
            for (int period = minPeriod; period < searchMaxPeriod; period++) {
                float diff = 0;
                int searchLength = buffer.length - period;

                // Unrolled loop for better performance (process 4 at a time)
                int i = 0;
                for (; i < searchLength - 3; i += 4) {
                    float delta0 = buffer[i] - buffer[i + period];
                    float delta1 = buffer[i + 1] - buffer[i + 1 + period];
                    float delta2 = buffer[i + 2] - buffer[i + 2 + period];
                    float delta3 = buffer[i + 3] - buffer[i + 3 + period];

                    diff += delta0 * delta0 + delta1 * delta1 + delta2 * delta2 + delta3 * delta3;

                    // Early exit if diff is already worse
                    if (diff > minDiff) {
                        break;
                    }
                }

                // Handle remaining samples
                for (; i < searchLength; i++) {
                    float delta = buffer[i] - buffer[i + period];
                    diff += delta * delta;
                }

                if (diff < minDiff) {
                    minDiff = diff;
                    bestFreq = sampleRate / period;
                }
            }

            return bestFreq;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error in autocorrelation", e);
            return 0;
        }
    }

    /**
     * Convert detected frequency to a DetectedNote with validation
     */
    public DetectedNote convertToNote(float frequency, float confidence) {
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
            LOGGER.fine("Frequency out of range: " + frequency);
            return null;
        }

        if (confidence < 0.0f || confidence > 1.0f) {
            LOGGER.warning("Invalid confidence value: " + confidence);
            confidence = Math.max(0.0f, Math.min(1.0f, confidence));
        }

        try {
            // Estimate which string and fret based on frequency
            int string = estimateString(frequency);
            int fret = estimateFret(frequency, string);

            // Validate results
            if (string < 1 || string > 6) {
                LOGGER.warning("Invalid string estimate: " + string);
                string = Math.max(1, Math.min(6, string));
            }

            if (fret < 0 || fret > 24) {
                LOGGER.fine("Fret out of range: " + fret);
                fret = Math.max(0, Math.min(24, fret));
            }

            long timestamp = System.currentTimeMillis();

            return new DetectedNote(frequency, string, fret, timestamp, confidence);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting frequency to note", e);
            return null;
        }
    }

    /**
     * Estimate which guitar string based on frequency
     */
    private int estimateString(float frequency) {
        // Standard tuning frequencies
        float[] openStrings = {82.41f, 110.0f, 146.83f, 196.0f, 246.94f, 329.63f};

        int closestString = 6;  // Default to low E
        float minDiff = Float.MAX_VALUE;

        for (int i = 0; i < openStrings.length; i++) {
            // Check if frequency is in range for this string (up to ~12 frets)
            float upperBound = openStrings[i] * 2.0f;  // One octave up

            if (frequency >= openStrings[i] && frequency <= upperBound) {
                float diff = Math.abs(frequency - openStrings[i]);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestString = 6 - i;  // String numbering: 1=high E, 6=low E
                }
            }
        }

        return closestString;
    }

    /**
     * Estimate fret position based on frequency and string
     */
    private int estimateFret(float frequency, int string) {
        float openStringFreq = frequencyConversion.getStringFrequency(string);

        // Each fret raises pitch by one semitone (factor of 2^(1/12))
        float ratio = frequency / openStringFreq;
        int fret = (int) Math.round(12 * Math.log(ratio) / Math.log(2));

        return Math.max(0, Math.min(24, fret));  // Clamp to 0-24 frets
    }

    /**
     * Filter noise from signal
     */
    public void filterNoise(float[] buffer) {
        // Simple high-pass filter to remove low-frequency noise
        final float ALPHA = 0.9f;
        float previousSample = 0;

        for (int i = 0; i < buffer.length; i++) {
            float current = buffer[i];
            buffer[i] = ALPHA * (buffer[i] + previousSample);
            previousSample = current;
        }
    }

    // Getters
    public float getLastDetectedFrequency() {
        return lastDetectedFrequency;
    }

    public long getLastDetectionTime() {
        return lastDetectionTime;
    }
}
