package com.shredforge.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores calibration offsets for each guitar string to improve note detection accuracy.
 * Created during the calibration process and used by FrequencyConversion.
 */
public class CalibrationData {
    // Standard guitar tuning frequencies (Hz) for open strings
    public static final float[] STANDARD_TUNING = {
        329.63f,  // String 1: E4 (high E)
        246.94f,  // String 2: B3
        196.00f,  // String 3: G3
        146.83f,  // String 4: D3
        110.00f,  // String 5: A2
        82.41f    // String 6: E2 (low E)
    };

    private final Map<Integer, Float> frequencyOffsets;  // String number -> frequency offset
    private boolean isCalibrated;
    private long calibrationTimestamp;

    public CalibrationData() {
        this.frequencyOffsets = new HashMap<>();
        this.isCalibrated = false;
        this.calibrationTimestamp = 0;
    }

    /**
     * Set the calibration offset for a specific string
     * @param stringNumber Guitar string (1-6)
     * @param detectedFrequency The actual frequency detected from the guitar
     */
    public void setStringOffset(int stringNumber, float detectedFrequency) {
        if (stringNumber < 1 || stringNumber > 6) {
            throw new IllegalArgumentException("String number must be between 1 and 6");
        }

        float expectedFrequency = STANDARD_TUNING[stringNumber - 1];
        float offset = detectedFrequency - expectedFrequency;
        frequencyOffsets.put(stringNumber, offset);
    }

    /**
     * Get the frequency offset for a specific string
     * @param stringNumber Guitar string (1-6)
     * @return Frequency offset in Hz, or 0.0 if not calibrated
     */
    public float getStringOffset(int stringNumber) {
        return frequencyOffsets.getOrDefault(stringNumber, 0.0f);
    }

    /**
     * Apply calibration offset to a detected frequency
     */
    public float applyCorrectionToFrequency(float detectedFrequency, int stringNumber) {
        float offset = getStringOffset(stringNumber);
        return detectedFrequency - offset;
    }

    /**
     * Check if calibration is complete for all strings
     */
    public boolean isComplete() {
        return frequencyOffsets.size() == 6;
    }

    /**
     * Mark calibration as complete and save timestamp
     */
    public void markAsCalibrated() {
        if (!isComplete()) {
            throw new IllegalStateException("Cannot mark as calibrated - not all strings calibrated");
        }
        this.isCalibrated = true;
        this.calibrationTimestamp = System.currentTimeMillis();
    }

    /**
     * Reset all calibration data
     */
    public void reset() {
        frequencyOffsets.clear();
        isCalibrated = false;
        calibrationTimestamp = 0;
    }

    // Getters
    public boolean isCalibrated() {
        return isCalibrated;
    }

    public long getCalibrationTimestamp() {
        return calibrationTimestamp;
    }

    public int getCalibratedStringCount() {
        return frequencyOffsets.size();
    }

    @Override
    public String toString() {
        return String.format("CalibrationData[calibrated=%s, strings=%d/6, timestamp=%d]",
                             isCalibrated, frequencyOffsets.size(), calibrationTimestamp);
    }
}
