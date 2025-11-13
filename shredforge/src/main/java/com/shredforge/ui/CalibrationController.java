package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.calibration.CalibrationService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.logging.Logger;

/**
 * Controller for Calibration interface.
 * Guides user through guitar input calibration process.
 * Per specification section 4.2.3
 */
public class CalibrationController {
    private static final Logger LOGGER = Logger.getLogger(CalibrationController.class.getName());

    @FXML
    private Label instructionLabel;

    @FXML
    private Label detectedFrequencyLabel;

    @FXML
    private Label tuningIndicator;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Rectangle audioLevelMeter;

    @FXML
    private Button startButton;

    @FXML
    private Button retryButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button saveExitButton;

    @FXML
    private Label statusLabel;

    private final CalibrationService calibrationService;
    private AnimationTimer calibrationTimer;
    private boolean isCalibrating;

    public CalibrationController() {
        this.calibrationService = new CalibrationService();
        this.isCalibrating = false;
    }

    @FXML
    private void initialize() {
        LOGGER.info("Calibration interface initialized");

        // Initial state
        if (retryButton != null) {
            retryButton.setDisable(true);
        }
        if (nextButton != null) {
            nextButton.setDisable(true);
        }
        if (saveExitButton != null) {
            saveExitButton.setDisable(true);
        }

        updateDisplay();
    }

    @FXML
    private void handleStart() {
        LOGGER.info("Starting calibration");

        boolean started = calibrationService.startCalibration();

        if (started) {
            isCalibrating = true;

            if (startButton != null) {
                startButton.setDisable(true);
            }
            if (retryButton != null) {
                retryButton.setDisable(false);
            }
            if (nextButton != null) {
                nextButton.setDisable(false);
            }

            startCalibrationTimer();
            showStatus("Calibration started - Play the indicated string");
        } else {
            showStatus("Failed to start calibration - Check audio input");
        }
    }

    @FXML
    private void handleNext() {
        LOGGER.info("Calibrating current string");
        showStatus("Calibrating...");

        // Run calibration in background
        new Thread(() -> {
            boolean success = calibrationService.calibrateCurrentString();

            Platform.runLater(() -> {
                if (success) {
                    if (calibrationService.isCalibrated()) {
                        // All strings calibrated
                        finishCalibration();
                    } else {
                        // Move to next string
                        updateDisplay();
                        showStatus("String calibrated! Play next string");
                    }
                } else {
                    showStatus("Calibration failed - Please retry");
                }
            });
        }).start();
    }

    @FXML
    private void handleRetry() {
        LOGGER.info("Retrying current string");
        updateDisplay();
        showStatus("Please play the indicated string again");
    }

    @FXML
    private void handleSaveExit() {
        LOGGER.info("Saving calibration and exiting");

        stopCalibrationTimer();
        isCalibrating = false;

        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        calibrationService.cancelCalibration();
        stopCalibrationTimer();
        isCalibrating = false;

        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    /**
     * Start animation timer for real-time feedback
     */
    private void startCalibrationTimer() {
        calibrationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateDisplay();
            }
        };
        calibrationTimer.start();
    }

    /**
     * Stop calibration timer
     */
    private void stopCalibrationTimer() {
        if (calibrationTimer != null) {
            calibrationTimer.stop();
        }
    }

    /**
     * Update display with current calibration state
     */
    private void updateDisplay() {
        if (!isCalibrating) return;

        // Update instruction
        if (instructionLabel != null) {
            instructionLabel.setText(calibrationService.getCurrentInstruction());
        }

        // Update progress
        if (progressBar != null) {
            progressBar.setProgress(calibrationService.getProgress() / 100.0);
        }

        if (progressLabel != null) {
            int current = 7 - calibrationService.getCurrentString();
            progressLabel.setText("String " + current + " of 6");
        }

        // Simulate frequency detection display
        if (detectedFrequencyLabel != null) {
            detectedFrequencyLabel.setText("--.- Hz");
        }

        if (tuningIndicator != null) {
            tuningIndicator.setText("♪");
            tuningIndicator.setTextFill(Color.GRAY);
        }

        // Simulate audio level
        if (audioLevelMeter != null) {
            audioLevelMeter.setWidth(50 + (Math.random() * 100));
        }
    }

    /**
     * Finish calibration process
     */
    private void finishCalibration() {
        stopCalibrationTimer();
        isCalibrating = false;

        if (instructionLabel != null) {
            instructionLabel.setText("Calibration Complete!");
            instructionLabel.setTextFill(Color.GREEN);
        }

        if (nextButton != null) {
            nextButton.setDisable(true);
        }

        if (retryButton != null) {
            retryButton.setDisable(true);
        }

        if (saveExitButton != null) {
            saveExitButton.setDisable(false);
        }

        showStatus("Calibration successful! You can now start practicing.");

        // Show success alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Calibration Complete");
        alert.setHeaderText(null);
        alert.setContentText("Your guitar has been calibrated successfully!\n\n" +
                           "You can now practice tabs with accurate note detection.");
        alert.showAndWait();
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        LOGGER.info(message);
    }
}
