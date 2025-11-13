package com.shredforge.persistence;

import com.shredforge.model.CalibrationData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistence of application data to disk with robust error handling.
 * Stores calibration data, settings, and other app state.
 * Implements backup mechanism for data safety.
 */
public class DataPersistence {
    private static final Logger LOGGER = Logger.getLogger(DataPersistence.class.getName());
    private static final String APP_DATA_DIR = System.getProperty("user.home") + "/.shredforge";
    private static final String CALIBRATION_FILE = "calibration.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static final String BACKUP_SUFFIX = ".backup";

    private final ObjectMapper objectMapper;
    private final Path dataDirectory;

    public DataPersistence() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.dataDirectory = Paths.get(APP_DATA_DIR);

        // Create data directory if it doesn't exist
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                LOGGER.info("Created data directory: " + dataDirectory);
            } else {
                LOGGER.info("Data directory: " + dataDirectory);
            }

            // Verify directory is writable
            if (!Files.isWritable(dataDirectory)) {
                LOGGER.severe("Data directory is not writable: " + dataDirectory);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create data directory", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error initializing data directory", e);
        }
    }

    /**
     * Save calibration data to disk with backup
     */
    public boolean saveCalibrationData(CalibrationData calibrationData) {
        if (calibrationData == null) {
            LOGGER.warning("Cannot save null calibration data");
            return false;
        }

        Path filePath = dataDirectory.resolve(CALIBRATION_FILE);
        Path backupPath = dataDirectory.resolve(CALIBRATION_FILE + BACKUP_SUFFIX);

        try {
            // Create backup of existing file before overwriting
            if (Files.exists(filePath)) {
                try {
                    Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.fine("Created backup: " + backupPath);
                } catch (IOException e) {
                    LOGGER.warning("Failed to create backup (continuing anyway): " + e.getMessage());
                }
            }

            // Create wrapper for JSON serialization
            CalibrationDataWrapper wrapper = new CalibrationDataWrapper(calibrationData);

            // Write to file
            objectMapper.writeValue(filePath.toFile(), wrapper);
            LOGGER.info("Calibration data saved to: " + filePath);
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save calibration data", e);

            // Try to restore from backup
            if (Files.exists(backupPath)) {
                try {
                    Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Restored from backup after save failure");
                } catch (IOException restoreError) {
                    LOGGER.log(Level.SEVERE, "Failed to restore backup", restoreError);
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving calibration data", e);
            return false;
        }
    }

    /**
     * Load calibration data from disk with fallback to backup if corrupted
     */
    public CalibrationData loadCalibrationData() {
        Path filePath = dataDirectory.resolve(CALIBRATION_FILE);
        Path backupPath = dataDirectory.resolve(CALIBRATION_FILE + BACKUP_SUFFIX);

        if (!Files.exists(filePath)) {
            LOGGER.info("No saved calibration data found");
            return null;
        }

        // Try loading from main file
        CalibrationData data = loadCalibrationDataFromFile(filePath);
        if (data != null) {
            return data;
        }

        // Main file failed, try backup
        LOGGER.warning("Main calibration file corrupted, trying backup");
        if (Files.exists(backupPath)) {
            data = loadCalibrationDataFromFile(backupPath);
            if (data != null) {
                LOGGER.info("Successfully loaded from backup");

                // Restore main file from backup
                try {
                    Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Restored main file from backup");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to restore main file from backup", e);
                }

                return data;
            }
        }

        LOGGER.severe("Both main and backup calibration files are corrupted");
        return null;
    }

    /**
     * Helper method to load calibration data from a specific file
     */
    private CalibrationData loadCalibrationDataFromFile(Path filePath) {
        try {
            File file = filePath.toFile();

            // Validate file
            if (!file.canRead()) {
                LOGGER.warning("Cannot read file: " + filePath);
                return null;
            }

            if (file.length() == 0) {
                LOGGER.warning("File is empty: " + filePath);
                return null;
            }

            // Parse JSON
            CalibrationDataWrapper wrapper = objectMapper.readValue(file, CalibrationDataWrapper.class);

            if (wrapper == null) {
                LOGGER.warning("Parsed wrapper is null");
                return null;
            }

            CalibrationData data = wrapper.toCalibrationData();
            LOGGER.info("Calibration data loaded from: " + filePath);
            return data;

        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "JSON parsing error for file: " + filePath, e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IO error loading file: " + filePath, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error loading file: " + filePath, e);
            return null;
        }
    }

    /**
     * Save application settings to disk with backup
     */
    public boolean saveSettings(AppSettings settings) {
        if (settings == null) {
            LOGGER.warning("Cannot save null settings");
            return false;
        }

        Path filePath = dataDirectory.resolve(SETTINGS_FILE);
        Path backupPath = dataDirectory.resolve(SETTINGS_FILE + BACKUP_SUFFIX);

        try {
            // Create backup of existing file
            if (Files.exists(filePath)) {
                try {
                    Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.fine("Created settings backup");
                } catch (IOException e) {
                    LOGGER.warning("Failed to create settings backup (continuing anyway): " + e.getMessage());
                }
            }

            // Write to file
            objectMapper.writeValue(filePath.toFile(), settings);
            LOGGER.info("Settings saved to: " + filePath);
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save settings", e);

            // Try to restore from backup
            if (Files.exists(backupPath)) {
                try {
                    Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Restored settings from backup after save failure");
                } catch (IOException restoreError) {
                    LOGGER.log(Level.SEVERE, "Failed to restore settings backup", restoreError);
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving settings", e);
            return false;
        }
    }

    /**
     * Load application settings from disk with fallback to backup if corrupted
     */
    public AppSettings loadSettings() {
        Path filePath = dataDirectory.resolve(SETTINGS_FILE);
        Path backupPath = dataDirectory.resolve(SETTINGS_FILE + BACKUP_SUFFIX);

        if (!Files.exists(filePath)) {
            LOGGER.info("No saved settings found, using defaults");
            return new AppSettings();
        }

        // Try loading from main file
        AppSettings settings = loadSettingsFromFile(filePath);
        if (settings != null) {
            return settings;
        }

        // Main file failed, try backup
        LOGGER.warning("Main settings file corrupted, trying backup");
        if (Files.exists(backupPath)) {
            settings = loadSettingsFromFile(backupPath);
            if (settings != null) {
                LOGGER.info("Successfully loaded settings from backup");

                // Restore main file from backup
                try {
                    Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Restored main settings file from backup");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to restore main settings file", e);
                }

                return settings;
            }
        }

        LOGGER.warning("Both main and backup settings files are corrupted, using defaults");
        return new AppSettings(); // Return defaults as last resort
    }

    /**
     * Helper method to load settings from a specific file
     */
    private AppSettings loadSettingsFromFile(Path filePath) {
        try {
            File file = filePath.toFile();

            // Validate file
            if (!file.canRead()) {
                LOGGER.warning("Cannot read file: " + filePath);
                return null;
            }

            if (file.length() == 0) {
                LOGGER.warning("File is empty: " + filePath);
                return null;
            }

            // Parse JSON
            AppSettings settings = objectMapper.readValue(file, AppSettings.class);

            if (settings == null) {
                LOGGER.warning("Parsed settings is null");
                return null;
            }

            LOGGER.info("Settings loaded from: " + filePath);
            return settings;

        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "JSON parsing error for settings file: " + filePath, e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IO error loading settings file: " + filePath, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error loading settings file: " + filePath, e);
            return null;
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
        public String theme = "light"; // "light" or "dark"
        public java.util.List<String> recentTabs = new java.util.ArrayList<>();
        public String lastPracticedTab = null;

        public AppSettings() {
        }
    }
}
