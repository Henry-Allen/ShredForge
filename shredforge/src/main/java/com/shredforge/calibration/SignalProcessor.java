package com.shredforge.calibration;

/**
 * Minimal abstraction used by the calibration service to take pitch measurements.
 */
public interface SignalProcessor {

    /**
     * @param stringName descriptive name so implementations can log or adjust heuristics
     * @param targetFrequencyHz desired reference frequency
     * @return measured frequency in Hz for the provided string
     */
    double measureFrequency(String stringName, double targetFrequencyHz);
}
