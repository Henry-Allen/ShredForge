package com.shredforge.input;

import com.shredforge.model.DetectedNote;
import com.shredforge.notedetection.FrequencyConversion;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.util.logging.Logger;

/**
 * Processes guitar audio signals to detect notes using TarsosDSP.
 * Uses Q-transform via YIN algorithm for accurate pitch detection.
 */
public class GuitarSignalProcessor {
    private static final Logger LOGGER = Logger.getLogger(GuitarSignalProcessor.class.getName());
    private static final float MIN_FREQUENCY = 80.0f;   // Low E string (E2)
    private static final float MAX_FREQUENCY = 1320.0f; // High E at 24th fret
    private static final float CONFIDENCE_THRESHOLD = 0.6f;

    private final float sampleRate;
    private final int bufferSize;
    private final FrequencyConversion frequencyConversion;

    private float lastDetectedFrequency;
    private long lastDetectionTime;

    public GuitarSignalProcessor(float sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.frequencyConversion = new FrequencyConversion();
        this.lastDetectedFrequency = 0.0f;
        this.lastDetectionTime = 0;
    }

    /**
     * Process audio signal and detect pitch
     * @param audioBuffer Audio samples
     * @return Detected frequency in Hz, or -1 if no clear pitch detected
     */
    public float processSignal(float[] audioBuffer) {
        if (audioBuffer == null || audioBuffer.length == 0) {
            return -1.0f;
        }

        // Apply simple noise gate
        float maxAmplitude = 0;
        for (float sample : audioBuffer) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
        }

        if (maxAmplitude < 0.01f) {  // Noise gate threshold
            return -1.0f;
        }

        // Use autocorrelation for pitch detection (simplified YIN)
        float frequency = detectPitchAutocorrelation(audioBuffer);

        // Filter out frequencies outside guitar range
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
            return -1.0f;
        }

        lastDetectedFrequency = frequency;
        lastDetectionTime = System.currentTimeMillis();

        return frequency;
    }

    /**
     * Simplified autocorrelation-based pitch detection
     * Based on YIN algorithm principles
     */
    private float detectPitchAutocorrelation(float[] buffer) {
        int minPeriod = (int) (sampleRate / MAX_FREQUENCY);
        int maxPeriod = (int) (sampleRate / MIN_FREQUENCY);

        float bestFreq = 0;
        float minDiff = Float.MAX_VALUE;

        // Autocorrelation
        for (int period = minPeriod; period < maxPeriod && period < buffer.length / 2; period++) {
            float diff = 0;

            for (int i = 0; i < buffer.length - period; i++) {
                float delta = buffer[i] - buffer[i + period];
                diff += delta * delta;
            }

            if (diff < minDiff) {
                minDiff = diff;
                bestFreq = sampleRate / period;
            }
        }

        return bestFreq;
    }

    /**
     * Convert detected frequency to a DetectedNote
     */
    public DetectedNote convertToNote(float frequency, float confidence) {
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
            return null;
        }

        // Estimate which string and fret based on frequency
        // This is simplified - real implementation would be more sophisticated
        int string = estimateString(frequency);
        int fret = estimateFret(frequency, string);

        long timestamp = System.currentTimeMillis();

        return new DetectedNote(frequency, string, fret, timestamp, confidence);
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
