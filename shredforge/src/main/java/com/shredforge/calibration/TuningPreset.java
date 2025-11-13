package com.shredforge.calibration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes a full-string tuning preset in Hz.
 */
public record TuningPreset(String name, Map<String, Double> stringFrequenciesHz) {

    public TuningPreset {
        Objects.requireNonNull(name, "name");
        stringFrequenciesHz = stringFrequenciesHz == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(stringFrequenciesHz));
    }

    @Override
    public String toString() {
        return name;
    }
}
