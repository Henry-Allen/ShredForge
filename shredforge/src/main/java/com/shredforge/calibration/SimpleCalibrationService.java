package com.shredforge.calibration;

import com.shredforge.core.model.CalibrationInput;
import com.shredforge.core.model.CalibrationProfile;
import com.shredforge.core.ports.CalibrationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight {@link CalibrationService} implementation that relies on a {@link SignalProcessor} to grab the current
 * pitch for each string and produces offsets in cents.
 */
public final class SimpleCalibrationService implements CalibrationService {

    private final SignalProcessor processor;

    public SimpleCalibrationService(SignalProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    @Override
    public CalibrationProfile calibrate(CalibrationInput input) {
        Map<String, Double> reference = input.stringReferenceHz();
        Map<String, Double> offsets = new LinkedHashMap<>();
        double maxOffset = 0.0;

        for (Map.Entry<String, Double> entry : reference.entrySet()) {
            String stringName = entry.getKey();
            double target = entry.getValue();
            double measured = processor.measureFrequency(stringName, target);
            double cents = 1200.0 * Math.log(measured / target) / Math.log(2);
            offsets.put(stringName, cents);
            maxOffset = Math.max(maxOffset, Math.abs(cents));
        }

        double gain = Math.max(20.0, 60.0 - input.ambientNoiseDb()) + maxOffset / 5.0;
        double noiseFloor = Math.max(5.0, input.ambientNoiseDb() - 3.0);

        return new CalibrationProfile(input.userId(), gain, noiseFloor, offsets, Instant.now());
    }
}
