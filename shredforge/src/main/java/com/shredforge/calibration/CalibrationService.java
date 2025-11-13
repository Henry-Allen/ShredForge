package com.shredforge.calibration;

import com.shredforge.model.CalibrationData;
import com.shredforge.input.AudioInput;
import com.shredforge.input.GuitarSignalProcessor;
import com.shredforge.repository.ShredForgeRepository;

import java.util.logging.Logger;

/**
 * Service for calibrating guitar input.
 * Guides user through playing reference notes to map frequencies to expected values.
 */
public class CalibrationService {
    private static final Logger LOGGER = Logger.getLogger(CalibrationService.class.getName());
    private static final int SAMPLES_PER_STRING = 5;  // Number of samples to average

    private final ShredForgeRepository repository;
    private final AudioInput audioInput;
    private final GuitarSignalProcessor signalProcessor;

    private CalibrationData calibrationData;
    private int currentString;  // 1-6
    private boolean isCalibrating;
    private String currentInstruction;

    public CalibrationService() {
        this.repository = ShredForgeRepository.getInstance();
        this.audioInput = new AudioInput();
        this.signalProcessor = new GuitarSignalProcessor(
            audioInput.getSampleRate(),
            audioInput.getBufferSize()
        );
        this.calibrationData = new CalibrationData();
        this.currentString = 6;  // Start with low E
        this.isCalibrating = false;
    }

    /**
     * Start calibration process
     */
    public boolean startCalibration() {
        if (isCalibrating) {
            LOGGER.warning("Calibration already in progress");
            return false;
        }

        if (!audioInput.openStream()) {
            LOGGER.severe("Failed to open audio stream for calibration");
            return false;
        }

        calibrationData = new CalibrationData();
        currentString = 6;  // Start with low E string
        isCalibrating = true;

        updateInstruction();
        LOGGER.info("Calibration started");
        return true;
    }

    /**
     * Calibrate current string
     * @return true if successful, false if needs retry
     */
    public boolean calibrateCurrentString() {
        if (!isCalibrating) {
            LOGGER.warning("Calibration not started");
            return false;
        }

        try {
            // Collect samples
            float[] frequencies = new float[SAMPLES_PER_STRING];
            int validSamples = 0;

            LOGGER.info("Calibrating string " + currentString + "...");

            for (int i = 0; i < SAMPLES_PER_STRING; i++) {
                float[] audioData = audioInput.readAudioData();

                if (audioData.length > 0) {
                    float frequency = signalProcessor.processSignal(audioData);

                    if (frequency > 0) {
                        frequencies[validSamples++] = frequency;
                    }
                }

                // Small delay between samples
                Thread.sleep(100);
            }

            if (validSamples < SAMPLES_PER_STRING / 2) {
                LOGGER.warning("Not enough valid samples, please retry");
                return false;
            }

            // Calculate average frequency
            float avgFrequency = 0;
            for (int i = 0; i < validSamples; i++) {
                avgFrequency += frequencies[i];
            }
            avgFrequency /= validSamples;

            // Save calibration offset for this string
            calibrationData.setStringOffset(currentString, avgFrequency);

            LOGGER.info(String.format("String %d calibrated: %.2f Hz", currentString, avgFrequency));

            // Move to next string
            if (currentString > 1) {
                currentString--;
                updateInstruction();
                return true;
            } else {
                // All strings calibrated
                finishCalibration();
                return true;
            }

        } catch (Exception e) {
            LOGGER.severe("Calibration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Skip current string (use default)
     */
    public void skipCurrentString() {
        if (currentString > 1) {
            currentString--;
            updateInstruction();
        } else {
            finishCalibration();
        }
    }

    /**
     * Finish calibration and save
     */
    private void finishCalibration() {
        if (calibrationData.isComplete()) {
            calibrationData.markAsCalibrated();
            repository.saveCalibrationData(calibrationData);
            LOGGER.info("Calibration complete!");
        }

        isCalibrating = false;
        audioInput.closeStream();

        currentInstruction = "Calibration Complete!";
    }

    /**
     * Cancel calibration
     */
    public void cancelCalibration() {
        isCalibrating = false;
        audioInput.closeStream();
        calibrationData.reset();
        LOGGER.info("Calibration cancelled");
    }

    /**
     * Update instruction text for current string
     */
    private void updateInstruction() {
        String[] stringNames = {
            "high E string (1st string)",
            "B string (2nd string)",
            "G string (3rd string)",
            "D string (4th string)",
            "A string (5th string)",
            "low E string (6th string)"
        };

        int index = 6 - currentString;
        if (index >= 0 && index < stringNames.length) {
            currentInstruction = "Play the " + stringNames[index] + " open";
        } else {
            currentInstruction = "Calibration in progress...";
        }
    }

    /**
     * Get frequency offset for string
     */
    public float getFrequencyOffset(int string) {
        return calibrationData.getStringOffset(string);
    }

    // Getters
    public boolean isCalibrating() {
        return isCalibrating;
    }

    public int getCurrentString() {
        return currentString;
    }

    public String getCurrentInstruction() {
        return currentInstruction;
    }

    public CalibrationData getCalibrationData() {
        return calibrationData;
    }

    public boolean isCalibrated() {
        return calibrationData.isCalibrated();
    }

    public int getProgress() {
        return (7 - currentString) * 100 / 6;
    }
}
