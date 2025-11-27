package com.shredforge.calibration;

import java.util.Objects;

/**
 * Represents a single guitar string with its expected tuning.
 * Used during the tuning calibration process.
 */
public record TuningString(
        int stringNumber,      // 1-6 for standard guitar (1 = high E, 6 = low E)
        String noteName,       // e.g., "E4", "B3", "G3", "D3", "A2", "E2"
        double targetFrequencyHz,
        int midiNote
) {
    public TuningString {
        Objects.requireNonNull(noteName, "noteName");
        if (stringNumber < 1 || stringNumber > 12) {
            throw new IllegalArgumentException("stringNumber must be 1-12");
        }
        if (targetFrequencyHz <= 0) {
            throw new IllegalArgumentException("targetFrequencyHz must be positive");
        }
    }

    /**
     * Creates a TuningString from a note name and frequency.
     */
    public static TuningString of(int stringNumber, String noteName, double frequencyHz) {
        int midi = frequencyToMidi(frequencyHz);
        return new TuningString(stringNumber, noteName, frequencyHz, midi);
    }

    /**
     * Converts frequency in Hz to MIDI note number.
     */
    private static int frequencyToMidi(double hz) {
        if (hz <= 0) return 0;
        return (int) Math.round(69 + 12 * (Math.log(hz / 440.0) / Math.log(2)));
    }

    /**
     * Calculates cents deviation from target frequency.
     * Positive = sharp, negative = flat.
     */
    public double centsFromTarget(double detectedFrequencyHz) {
        if (detectedFrequencyHz <= 0) return Double.NaN;
        return 1200 * Math.log(detectedFrequencyHz / targetFrequencyHz) / Math.log(2);
    }

    /**
     * Returns true if the detected frequency is within the given cents tolerance.
     */
    public boolean isInTune(double detectedFrequencyHz, double centsTolerance) {
        double cents = centsFromTarget(detectedFrequencyHz);
        return !Double.isNaN(cents) && Math.abs(cents) <= centsTolerance;
    }

    @Override
    public String toString() {
        return String.format("String %d: %s (%.2f Hz)", stringNumber, noteName, targetFrequencyHz);
    }
}
