package com.shredforge.persistence;

import com.shredforge.model.CalibrationData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Handles persistence of application data to disk.
 * Stores calibration data, settings, and other app state.
 */
public class DataPersistence {
    private static final Logger LOGGER = Logger.getLogger(DataPersistence.class.getName());
    private static final String APP_DATA_DIR = System.getProperty("user.home") + "/.shredforge";
    private static final String CALIBRATION_FILE = "calibration.json";
    private static final String SETTINGS_FILE = "settings.json";

    private final ObjectMapper objectMapper;
    private final Path dataDirectory;

    public DataPersistence() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.dataDirectory = Paths.get(APP_DATA_DIR);

        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(dataDirectory);
            LOGGER.info("Data directory: " + dataDirectory);
        } catch (IOException e) {
            LOGGER.severe("Failed to create data directory: " + e.getMessage());
        }
    }

    /**
     * Save calibration data to disk
     */
    public boolean saveCalibrationData(CalibrationData calibrationData) {
        if (calibrationData == null) {
            LOGGER.warning("Cannot save null calibration data");
            return false;
        }

        try {
            File file = dataDirectory.resolve(CALIBRATION_FILE).toFile();

            // Create a simple wrapper for JSON serialization
            CalibrationDataWrapper wrapper = new CalibrationDataWrapper(calibrationData);

            objectMapper.writeValue(file, wrapper);
            LOGGER.info("Calibration data saved to: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            LOGGER.severe("Failed to save calibration data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load calibration data from disk
     */
    public CalibrationData loadCalibrationData() {
        try {
            File file = dataDirectory.resolve(CALIBRATION_FILE).toFile();

            if (!file.exists()) {
                LOGGER.info("No saved calibration data found");
                return null;
            }

            CalibrationDataWrapper wrapper = objectMapper.readValue(file, CalibrationDataWrapper.class);
            CalibrationData data = wrapper.toCalibrationData();

            LOGGER.info("Calibration data loaded from disk");
            return data;

        } catch (IOException e) {
            LOGGER.warning("Failed to load calibration data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save application settings to disk
     */
    public boolean saveSettings(AppSettings settings) {
        try {
            File file = dataDirectory.resolve(SETTINGS_FILE).toFile();
            objectMapper.writeValue(file, settings);
            LOGGER.info("Settings saved to: " + file.getAbsolutePath());
            return true;

        } catch (IOException e) {
            LOGGER.severe("Failed to save settings: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load application settings from disk
     */
    public AppSettings loadSettings() {
        try {
            File file = dataDirectory.resolve(SETTINGS_FILE).toFile();

            if (!file.exists()) {
                LOGGER.info("No saved settings found, using defaults");
                return new AppSettings(); // Return defaults
            }

            AppSettings settings = objectMapper.readValue(file, AppSettings.class);
            LOGGER.info("Settings loaded from disk");
            return settings;

        } catch (IOException e) {
            LOGGER.warning("Failed to load settings: " + e.getMessage());
            return new AppSettings(); // Return defaults
        }
    }

    /**
     * Get data directory path
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Wrapper class for JSON serialization of CalibrationData
     */
    private static class CalibrationDataWrapper {
        public boolean isCalibrated;
        public long calibrationTimestamp;
        public float[] stringOffsets; // Array of 6 offsets

        public CalibrationDataWrapper() {
            this.stringOffsets = new float[6];
        }

        public CalibrationDataWrapper(CalibrationData data) {
            this.isCalibrated = data.isCalibrated();
            this.calibrationTimestamp = data.getCalibrationTimestamp();
            this.stringOffsets = new float[6];

            // Extract offsets for all strings
            for (int i = 1; i <= 6; i++) {
                this.stringOffsets[i - 1] = data.getStringOffset(i);
            }
        }

        public CalibrationData toCalibrationData() {
            CalibrationData data = new CalibrationData();

            // Restore offsets
            for (int i = 0; i < 6 && i < stringOffsets.length; i++) {
                if (stringOffsets[i] != 0) {
                    // Calculate detected frequency from offset
                    float expectedFreq = CalibrationData.STANDARD_TUNING[i];
                    float detectedFreq = expectedFreq + stringOffsets[i];
                    data.setStringOffset(i + 1, detectedFreq);
                }
            }

            if (isCalibrated) {
                data.markAsCalibrated();
            }

            return data;
        }
    }

    /**
     * Application settings class
     */
    public static class AppSettings {
        public String audioInputDevice = "default";
        public float masterVolume = 0.8f;
        public float detectionSensitivity = 0.6f;
        public boolean autoScroll = true;
        public boolean showTimingErrors = true;

        public AppSettings() {
        }
    }
}
