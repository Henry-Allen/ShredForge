package com.shredforge.core.model;

import java.util.Objects;

/**
 * Configuration for a practice session including audio detection settings.
 */
public record PracticeConfig(
        AudioDeviceInfo audioDevice,
        double pitchToleranceCents,
        double timingToleranceMs,
        double minimumConfidence,
        float sampleRate,
        int bufferSize,
        int cqtBinsPerOctave,
        double latencyCompensationMs) {

    public PracticeConfig {
        Objects.requireNonNull(audioDevice, "audioDevice");
        if (pitchToleranceCents < 0) {
            throw new IllegalArgumentException("pitchToleranceCents must be >= 0");
        }
        if (timingToleranceMs < 0) {
            throw new IllegalArgumentException("timingToleranceMs must be >= 0");
        }
        if (minimumConfidence < 0 || minimumConfidence > 1) {
            throw new IllegalArgumentException("minimumConfidence must be between 0 and 1");
        }
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be > 0");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize must be > 0");
        }
    }

    /**
     * Creates a default configuration suitable for guitar via audio interface.
     */
    public static PracticeConfig defaults() {
        return new PracticeConfig(
                AudioDeviceInfo.systemDefault(),
                50.0,      // 50 cents pitch tolerance (half semitone)
                150.0,     // 150ms timing tolerance
                0.7,       // 70% minimum confidence
                44100f,    // 44.1kHz sample rate
                4096,      // Buffer size for CQT (needs to be larger for low frequencies)
                36,        // 36 bins per octave (3 per semitone) for CQT
                0.0        // No latency compensation by default
        );
    }

    /**
     * Creates a new config with the specified audio device.
     */
    public PracticeConfig withAudioDevice(AudioDeviceInfo device) {
        return new PracticeConfig(device, pitchToleranceCents, timingToleranceMs, minimumConfidence,
                sampleRate, bufferSize, cqtBinsPerOctave, latencyCompensationMs);
    }

    /**
     * Creates a new config with the specified confidence threshold.
     */
    public PracticeConfig withConfidence(double confidence) {
        return new PracticeConfig(audioDevice, pitchToleranceCents, timingToleranceMs, confidence,
                sampleRate, bufferSize, cqtBinsPerOctave, latencyCompensationMs);
    }

    /**
     * Creates a new config with the specified pitch tolerance.
     */
    public PracticeConfig withPitchTolerance(double cents) {
        return new PracticeConfig(audioDevice, cents, timingToleranceMs, minimumConfidence,
                sampleRate, bufferSize, cqtBinsPerOctave, latencyCompensationMs);
    }

    /**
     * Creates a new config with the specified timing tolerance.
     */
    public PracticeConfig withTimingTolerance(double ms) {
        return new PracticeConfig(audioDevice, pitchToleranceCents, ms, minimumConfidence,
                sampleRate, bufferSize, cqtBinsPerOctave, latencyCompensationMs);
    }

    /**
     * Creates a new config with the specified latency compensation.
     */
    public PracticeConfig withLatencyCompensation(double ms) {
        return new PracticeConfig(audioDevice, pitchToleranceCents, timingToleranceMs, minimumConfidence,
                sampleRate, bufferSize, cqtBinsPerOctave, ms);
    }
}
