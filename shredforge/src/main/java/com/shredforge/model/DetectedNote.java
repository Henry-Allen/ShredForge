package com.shredforge.model;

/**
 * Represents a note detected from live guitar audio input.
 * Extends Note with confidence and raw frequency information.
 * Used by the Note Detection subsystem.
 */
public class DetectedNote extends Note {
    private final float frequency;      // Raw detected frequency in Hz
    private final float confidence;     // Detection confidence (0.0-1.0)

    public DetectedNote(float frequency, int string, int fret, long timestamp, float confidence) {
        super(Note.fromFrequency(frequency, string, fret, timestamp).getNoteName(),
              Note.fromFrequency(frequency, string, fret, timestamp).getOctave(),
              string, fret, timestamp);
        this.frequency = frequency;
        this.confidence = confidence;
    }

    /**
     * Checks if this detected note matches an expected note within timing and pitch tolerance
     */
    @Override
    public boolean matches(Note expected) {
        // Check pitch match
        boolean pitchMatch = super.matches(expected);

        // Check if confidence is high enough
        boolean confidentEnough = confidence > 0.6f;

        return pitchMatch && confidentEnough;
    }

    /**
     * Check if the note was detected with low confidence (possible incorrect detection)
     */
    public boolean isLowConfidence() {
        return confidence < 0.6f;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" [%.2fHz, conf:%.2f]", frequency, confidence);
    }
}
