package com.shredforge.notedetection;

import com.shredforge.model.Note;
import com.shredforge.model.CalibrationData;
import com.shredforge.repository.ShredForgeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Converts between frequencies and musical notes.
 * Applies calibration offsets for improved accuracy.
 */
public class FrequencyConversion {
    private static final Logger LOGGER = Logger.getLogger(FrequencyConversion.class.getName());
    private static final float REFERENCE_FREQ = 440.0f;  // A4 standard tuning
    private static final float TOLERANCE_CENTS = 50.0f;   // ±50 cents tolerance

    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private final ShredForgeRepository repository;

    public FrequencyConversion() {
        this.repository = ShredForgeRepository.getInstance();
    }

    /**
     * Convert frequency to Note
     * @param frequency Detected frequency in Hz
     * @return Note object, or null if frequency is out of range
     */
    public Note frequencyToNote(float frequency) {
        if (frequency <= 0) {
            return null;
        }

        // Calculate semitones from A4 (440 Hz)
        double semitones = 12 * Math.log(frequency / REFERENCE_FREQ) / Math.log(2);
        int semitonesFromA4 = (int) Math.round(semitones);

        // A4 is the 9th note (index 9) in octave 4
        int noteIndex = (9 + semitonesFromA4) % 12;
        if (noteIndex < 0) noteIndex += 12;

        int octave = 4 + (9 + semitonesFromA4) / 12;

        String noteName = NOTE_NAMES[noteIndex];

        // Estimate string and fret (simplified)
        int string = estimateString(frequency);
        int fret = estimateFret(frequency, string);
        long timestamp = System.currentTimeMillis();

        return new Note(noteName, octave, string, fret, timestamp);
    }

    /**
     * Get the closest note within tolerance
     * @param frequency Detected frequency
     * @return Note if within tolerance, null otherwise
     */
    public Note getClosestNote(float frequency) {
        Note note = frequencyToNote(frequency);

        if (note == null) {
            return null;
        }

        // Check if frequency is within tolerance of the note
        float expectedFreq = note.getFrequency();
        float centsDiff = 1200 * (float) (Math.log(frequency / expectedFreq) / Math.log(2));

        if (Math.abs(centsDiff) > TOLERANCE_CENTS) {
            return null;  // Too far from any note
        }

        return note;
    }

    /**
     * Get frequency for a specific string's open note
     */
    public float getStringFrequency(int string) {
        // Standard tuning: E2, A2, D3, G3, B3, E4 (from string 6 to 1)
        float[] openStrings = {329.63f, 246.94f, 196.0f, 146.83f, 110.0f, 82.41f};

        if (string < 1 || string > 6) {
            LOGGER.warning("Invalid string number: " + string);
            return 0;
        }

        return openStrings[string - 1];
    }

    /**
     * Apply calibration offset to frequency
     */
    public float applyCalibratedFrequency(float detectedFrequency, int string) {
        CalibrationData calibration = repository.getCalibrationData();

        if (calibration != null && calibration.isCalibrated()) {
            return calibration.applyCorrectionToFrequency(detectedFrequency, string);
        }

        return detectedFrequency;
    }

    /**
     * Estimate guitar string from frequency
     */
    private int estimateString(float frequency) {
        float[] openStrings = {329.63f, 246.94f, 196.0f, 146.83f, 110.0f, 82.41f};

        int closestString = 1;
        float minDiff = Float.MAX_VALUE;

        for (int i = 0; i < openStrings.length; i++) {
            float upperBound = openStrings[i] * 2.0f;  // One octave up

            if (frequency >= openStrings[i] * 0.95f && frequency <= upperBound) {
                float diff = Math.abs(frequency - openStrings[i]);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestString = i + 1;
                }
            }
        }

        return closestString;
    }

    /**
     * Estimate fret position from frequency and string
     */
    private int estimateFret(float frequency, int string) {
        float openStringFreq = getStringFrequency(string);

        if (openStringFreq == 0) {
            return 0;
        }

        // Each fret raises pitch by one semitone (2^(1/12))
        float ratio = frequency / openStringFreq;
        int fret = (int) Math.round(12 * Math.log(ratio) / Math.log(2));

        return Math.max(0, Math.min(24, fret));
    }

    /**
     * Calculate frequency difference in cents
     */
    public float getCentsDifference(float detected, float expected) {
        return 1200 * (float) (Math.log(detected / expected) / Math.log(2));
    }

    /**
     * Check if frequency matches expected note within tolerance
     */
    public boolean isWithinTolerance(float detected, float expected) {
        float cents = Math.abs(getCentsDifference(detected, expected));
        return cents <= TOLERANCE_CENTS;
    }
}
