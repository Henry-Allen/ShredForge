package com.shredforge.scoring.detection;

public record NoteDetectionConfig(
        float sampleRate,
        int bufferSize,
        int overlap,
        double pitchToleranceCents,
        double timingToleranceMillis,
        double minimumConfidence) {

    public static NoteDetectionConfig defaults() {
        return new NoteDetectionConfig(44100f, 2048, 1024, 35.0, 180.0, 0.6);
    }
}
