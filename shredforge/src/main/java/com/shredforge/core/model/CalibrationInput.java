package com.shredforge.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Input collected from the user to build a calibration profile.
 */
public record CalibrationInput(String userId, double ambientNoiseDb, Map<String, Double> stringReferenceHz) {

    public CalibrationInput {
        Objects.requireNonNull(userId, "userId");
        stringReferenceHz = stringReferenceHz == null ? Map.of() : Map.copyOf(stringReferenceHz);
    }
}
