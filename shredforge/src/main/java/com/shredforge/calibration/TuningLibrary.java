package com.shredforge.calibration;

import java.util.List;
import java.util.Map;

/**
 * Shared repository of tuning presets that the UI and calibration service can reference.
 */
public final class TuningLibrary {

    private static final List<TuningPreset> COMMON_TUNINGS = List.of(
            new TuningPreset(
                    "Standard (EADGBE)",
                    Map.of(
                            "E2", 82.41,
                            "A2", 110.0,
                            "D3", 146.83,
                            "G3", 196.0,
                            "B3", 246.94,
                            "E4", 329.63)),
            new TuningPreset(
                    "Drop D",
                    Map.of(
                            "D2", 73.42,
                            "A2", 110.0,
                            "D3", 146.83,
                            "G3", 196.0,
                            "B3", 246.94,
                            "E4", 329.63)),
            new TuningPreset(
                    "Eb Standard",
                    Map.of(
                            "Eb2", 77.78,
                            "Ab2", 103.83,
                            "Db3", 138.59,
                            "Gb3", 185.0,
                            "Bb3", 233.08,
                            "Eb4", 311.13)),
            new TuningPreset(
                    "DADGAD",
                    Map.of(
                            "D2", 73.42,
                            "A2", 110.0,
                            "D3", 146.83,
                            "G3", 196.0,
                            "A3", 220.0,
                            "D4", 293.66)));

    private TuningLibrary() {}

    public static List<TuningPreset> commonTunings() {
        return COMMON_TUNINGS;
    }
}
