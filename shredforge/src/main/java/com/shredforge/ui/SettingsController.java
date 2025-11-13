package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.input.AudioInput;
import com.shredforge.repository.ShredForgeRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.logging.Logger;

/**
 * Controller for Settings interface.
 * Allows user to configure audio devices, detection sensitivity, and preferences.
 */
public class SettingsController {
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    @FXML
    private ChoiceBox<String> audioDeviceChoice;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label volumeLabel;

    @FXML
    private Slider detectionSensitivitySlider;

    @FXML
    private Label sensitivityLabel;

    @FXML
    private CheckBox autoScrollCheckbox;

    @FXML
    private CheckBox showTimingErrorsCheckbox;

    @FXML
    private Button testAudioButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label currentDeviceLabel;

    @FXML
    private ProgressBar audioLevelMeter;

    private final ShredForgeRepository repository;
    private final ObservableList<String> audioDevices;

    public SettingsController() {
        this.repository = ShredForgeRepository.getInstance();
        this.audioDevices = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        LOGGER.info("Settings initialized");

        // Load audio devices
        loadAudioDevices();

        // Set up volume slider
        if (volumeSlider != null) {
            volumeSlider.setMin(0);
            volumeSlider.setMax(100);
            volumeSlider.setValue(repository.getMasterVolume() * 100);
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateVolumeLabel();
            });
        }

        // Set up detection sensitivity slider
        if (detectionSensitivitySlider != null) {
            detectionSensitivitySlider.setMin(0.3);
            detectionSensitivitySlider.setMax(0.9);
            detectionSensitivitySlider.setValue(0.6);
            detectionSensitivitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateSensitivityLabel();
            });
        }

        // Set up checkboxes
        if (autoScrollCheckbox != null) {
            autoScrollCheckbox.setSelected(true);
        }

        if (showTimingErrorsCheckbox != null) {
            showTimingErrorsCheckbox.setSelected(true);
        }

        // Load current settings
        loadSettings();

        // Initial label updates
        updateVolumeLabel();
        updateSensitivityLabel();
        updateCurrentDevice();
    }

    private void loadAudioDevices() {
        audioDevices.clear();
        audioDevices.add("Default System Device");

        try {
            String[] devices = AudioInput.getAvailableDevices();
            for (String device : devices) {
                if (!device.isEmpty()) {
                    audioDevices.add(device);
                }
            }

            if (audioDeviceChoice != null) {
                audioDeviceChoice.setItems(audioDevices);
                audioDeviceChoice.setValue("Default System Device");
            }

            LOGGER.info("Loaded " + audioDevices.size() + " audio devices");

        } catch (Exception e) {
            LOGGER.severe("Failed to load audio devices: " + e.getMessage());
            showStatus("Failed to load audio devices");
        }
    }

    private void loadSettings() {
        // Load saved audio device
        String savedDevice = repository.getAudioInputDevice();
        if (audioDeviceChoice != null && audioDevices.contains(savedDevice)) {
            audioDeviceChoice.setValue(savedDevice);
        }

        // Load volume
        if (volumeSlider != null) {
            volumeSlider.setValue(repository.getMasterVolume() * 100);
        }
    }

    @FXML
    private void handleTestAudio() {
        LOGGER.info("Testing audio input");
        showStatus("Testing audio input...");

        if (testAudioButton != null) {
            testAudioButton.setDisable(true);
        }

        // Test audio in background thread
        new Thread(() -> {
            try {
                AudioInput testInput = new AudioInput();
                boolean opened = testInput.openStream();

                if (opened) {
                    // Read a few samples
                    for (int i = 0; i < 10; i++) {
                        float[] data = testInput.readAudioData();

                        // Calculate level
                        float maxLevel = 0;
                        for (float sample : data) {
                            maxLevel = Math.max(maxLevel, Math.abs(sample));
                        }

                        final float level = maxLevel;
                        javafx.application.Platform.runLater(() -> {
                            if (audioLevelMeter != null) {
                                audioLevelMeter.setProgress(level);
                            }
                        });

                        Thread.sleep(100);
                    }

                    testInput.closeStream();

                    javafx.application.Platform.runLater(() -> {
                        showStatus("Audio test successful!");
                        if (testAudioButton != null) {
                            testAudioButton.setDisable(false);
                        }
                    });

                } else {
                    javafx.application.Platform.runLater(() -> {
                        showStatus("Failed to open audio device. Check connection.");
                        if (testAudioButton != null) {
                            testAudioButton.setDisable(false);
                        }
                    });
                }

            } catch (Exception e) {
                LOGGER.severe("Audio test failed: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    showStatus("Audio test failed: " + e.getMessage());
                    if (testAudioButton != null) {
                        testAudioButton.setDisable(false);
                    }
                });
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        LOGGER.info("Saving settings");

        try {
            // Save audio device
            if (audioDeviceChoice != null && audioDeviceChoice.getValue() != null) {
                String device = audioDeviceChoice.getValue();
                repository.setAudioInputDevice(device);
                LOGGER.info("Audio device set to: " + device);
            }

            // Save volume
            if (volumeSlider != null) {
                float volume = (float) (volumeSlider.getValue() / 100.0);
                repository.setMasterVolume(volume);
                LOGGER.info("Volume set to: " + volume);
            }

            // Update display
            updateCurrentDevice();
            showStatus("Settings saved successfully!");

            // Show confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Settings Saved");
            alert.setHeaderText(null);
            alert.setContentText("Your settings have been saved.\n\nChanges will take effect on next session.");
            alert.showAndWait();

        } catch (Exception e) {
            LOGGER.severe("Failed to save settings: " + e.getMessage());
            showStatus("Failed to save settings");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Failed");
            alert.setHeaderText("Failed to save settings");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleReset() {
        LOGGER.info("Resetting settings to defaults");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Settings");
        confirm.setHeaderText("Reset to Defaults?");
        confirm.setContentText("This will reset all settings to their default values.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Reset to defaults
                if (audioDeviceChoice != null) {
                    audioDeviceChoice.setValue("Default System Device");
                }

                if (volumeSlider != null) {
                    volumeSlider.setValue(80);
                }

                if (detectionSensitivitySlider != null) {
                    detectionSensitivitySlider.setValue(0.6);
                }

                if (autoScrollCheckbox != null) {
                    autoScrollCheckbox.setSelected(true);
                }

                if (showTimingErrorsCheckbox != null) {
                    showTimingErrorsCheckbox.setSelected(true);
                }

                showStatus("Settings reset to defaults");
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void updateVolumeLabel() {
        if (volumeLabel != null && volumeSlider != null) {
            volumeLabel.setText(String.format("%.0f%%", volumeSlider.getValue()));
        }
    }

    private void updateSensitivityLabel() {
        if (sensitivityLabel != null && detectionSensitivitySlider != null) {
            double value = detectionSensitivitySlider.getValue();
            String level;
            if (value < 0.4) {
                level = "Low";
            } else if (value < 0.7) {
                level = "Medium";
            } else {
                level = "High";
            }
            sensitivityLabel.setText(String.format("%.2f (%s)", value, level));
        }
    }

    private void updateCurrentDevice() {
        if (currentDeviceLabel != null) {
            currentDeviceLabel.setText("Current: " + repository.getAudioInputDevice());
        }
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        LOGGER.info(message);
    }
}
