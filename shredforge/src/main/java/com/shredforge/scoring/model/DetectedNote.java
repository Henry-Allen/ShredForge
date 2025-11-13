package com.shredforge.scoring.model;

import java.util.Locale;

/**
 * Represents a note detected from live guitar input.
 */
public record DetectedNote(
        double timestampMillis,
        double frequencyHz,
        double midi,
        double centsFromReference,
        double confidence,
        String noteName) {

    public DetectedNote {
        if (Double.isNaN(timestampMillis) || Double.isInfinite(timestampMillis)) {
            throw new IllegalArgumentException("timestampMillis must be finite");
        }
        if (Double.isNaN(frequencyHz) || frequencyHz < 0) {
            throw new IllegalArgumentException("frequencyHz must be >= 0");
        }
        if (noteName == null || noteName.isBlank()) {
            noteName = midiToNoteName(midi);
        }
    }

    public boolean isSilence() {
        return frequencyHz <= 0.0;
    }

    public static String midiToNoteName(double midi) {
        if (Double.isNaN(midi)) {
            return "--";
        }
        int rounded = (int) Math.round(midi);
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int noteIndex = Math.floorMod(rounded, 12);
        int octave = (rounded / 12) - 1;
        return String.format(Locale.US, "%s%d", names[noteIndex], octave);
    }
}
