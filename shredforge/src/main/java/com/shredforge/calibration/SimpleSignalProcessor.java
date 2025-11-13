package com.shredforge.calibration;

import java.util.Random;

/**
 * Lightweight, fake "analyzer" that simulates a pitch detector by returning values close to the requested target.
 * This keeps the calibration UI deterministic and unit-test friendly without pulling in the heavier DSP stack.
 */
public final class SimpleSignalProcessor implements SignalProcessor {

    private final Random random = new Random();

    @Override
    public double measureFrequency(String stringName, double targetFrequencyHz) {
        double centsOffset = (random.nextDouble() - 0.5) * 18; // +/- 9 cents
        double ratio = Math.pow(2.0, centsOffset / 1200.0);
        return targetFrequencyHz * ratio;
    }
}
