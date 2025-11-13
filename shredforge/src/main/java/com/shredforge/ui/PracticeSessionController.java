package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.Session;
import com.shredforge.model.Tab;
import com.shredforge.model.Note;
import com.shredforge.model.DetectedNote;
import com.shredforge.notedetection.NoteDetectionEngine;
import com.shredforge.playback.PlaybackController;
import com.shredforge.playback.ScoreCalculator;
import com.shredforge.repository.ShredForgeRepository;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

/**
 * Controller for Practice Session interface.
 * Provides real-time tab display with note detection feedback.
 * Per specification section 4.2.4
 */
public class PracticeSessionController {
    private static final Logger LOGGER = Logger.getLogger(PracticeSessionController.class.getName());

    @FXML
    private Canvas tabCanvas;

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button stopButton;

    @FXML
    private Slider tempoSlider;

    @FXML
    private Label tempoLabel;

    @FXML
    private Label accuracyLabel;

    @FXML
    private Label notesCompletedLabel;

    @FXML
    private Label streakLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label statusLabel;

    private final ShredForgeRepository repository;
    private final PlaybackController playbackController;
    private final NoteDetectionEngine noteDetectionEngine;
    private final ScoreCalculator scoreCalculator;

    private Session currentSession;
    private Tab currentTab;
    private AnimationTimer updateTimer;
    private long sessionStartTime;
    private boolean isPlaying;

    public PracticeSessionController() {
        this.repository = ShredForgeRepository.getInstance();
        this.playbackController = new PlaybackController();
        this.noteDetectionEngine = new NoteDetectionEngine();
        this.scoreCalculator = new ScoreCalculator();
        this.isPlaying = false;
    }

    @FXML
    private void initialize() {
        LOGGER.info("Practice Session initialized");

        // Load current tab from repository
        currentTab = repository.getCurrentTab();

        if (currentTab == null) {
            showStatus("No tab loaded. Please select a tab first.");
            disableControls();
            return;
        }

        // Check calibration
        if (!repository.isCalibrated()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Calibration Required");
            alert.setHeaderText("Guitar Not Calibrated");
            alert.setContentText("For best results, please calibrate your guitar before practicing.\n\nDo you want to continue anyway?");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.NO) {
                    handleBack();
                }
            });
        }

        // Set up tempo slider
        if (tempoSlider != null) {
            tempoSlider.setMin(50);
            tempoSlider.setMax(100);
            tempoSlider.setValue(100);
            tempoSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                float speed = newVal.floatValue() / 100.0f;
                playbackController.setSpeed(speed);
                updateTempoLabel();
            });
        }

        // Initialize session
        initializeSession();

        // Set up canvas
        setupCanvas();

        // Initial display
        updateDisplay();
        showStatus("Ready to practice: " + currentTab.getTitle());
    }

    private void initializeSession() {
        currentSession = repository.createSession(currentTab);
        playbackController.loadTab(currentTab);
        scoreCalculator.setTotalNotes(currentTab.getTotalNotes());
        sessionStartTime = System.currentTimeMillis();
        LOGGER.info("Session initialized for: " + currentTab.getTitle());
    }

    private void setupCanvas() {
        if (tabCanvas != null) {
            // Draw initial tab visualization
            drawTab();
        }
    }

    private void drawTab() {
        if (tabCanvas == null) return;

        GraphicsContext gc = tabCanvas.getGraphicsContext2D();

        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, tabCanvas.getWidth(), tabCanvas.getHeight());

        // Draw tab title
        gc.setFill(Color.BLACK);
        gc.fillText(currentTab.getTitle() + " - " + currentTab.getArtist(), 20, 30);

        // Draw simplified tab representation
        gc.setStroke(Color.BLACK);
        for (int i = 0; i < 6; i++) {
            double y = 80 + (i * 30);
            gc.strokeLine(50, y, tabCanvas.getWidth() - 50, y);
        }

        // Draw position marker
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);
        double centerX = tabCanvas.getWidth() / 2;
        gc.strokeLine(centerX, 60, centerX, 240);

        gc.fillText("♪ Tab visualization - Real-time feedback will appear here", 100, 280);
    }

    @FXML
    private void handlePlay() {
        if (currentTab == null) {
            showStatus("No tab loaded");
            return;
        }

        LOGGER.info("Starting practice session");
        isPlaying = true;

        // Start playback
        playbackController.start();

        // Start note detection
        noteDetectionEngine.startDetection();

        // Start session timer
        sessionStartTime = System.currentTimeMillis();

        // Start update timer
        startUpdateTimer();

        // Update UI
        if (playButton != null) playButton.setDisable(true);
        if (pauseButton != null) pauseButton.setDisable(false);
        if (stopButton != null) stopButton.setDisable(false);

        showStatus("Playing - Listen and play along!");
    }

    @FXML
    private void handlePause() {
        LOGGER.info("Pausing session");

        if (playbackController.isPlaying()) {
            playbackController.pause();
            showStatus("Paused");
        } else {
            playbackController.resume();
            showStatus("Resumed");
        }
    }

    @FXML
    private void handleStop() {
        LOGGER.info("Stopping session");
        stopSession();

        // Show score report
        try {
            currentSession.complete();
            repository.endSession();
            App.setRoot("scorereport");
        } catch (Exception e) {
            LOGGER.severe("Failed to show score report: " + e.getMessage());
            handleBack();
        }
    }

    @FXML
    private void handleBack() {
        stopSession();
        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void stopSession() {
        isPlaying = false;

        // Stop playback
        playbackController.stop();

        // Stop note detection
        noteDetectionEngine.stopDetection();

        // Stop update timer
        if (updateTimer != null) {
            updateTimer.stop();
        }

        showStatus("Session stopped");
    }

    private void startUpdateTimer() {
        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // Update every 100ms
                if (now - lastUpdate >= 100_000_000) {
                    updateDisplay();
                    processNoteDetection();
                    lastUpdate = now;
                }
            }
        };
        updateTimer.start();
    }

    private void updateDisplay() {
        Platform.runLater(() -> {
            // Update accuracy
            if (accuracyLabel != null) {
                accuracyLabel.setText(String.format("%.1f%%", scoreCalculator.getAccuracyPercentage()));
            }

            // Update notes completed
            if (notesCompletedLabel != null) {
                int completed = scoreCalculator.getCorrectNotes() + scoreCalculator.getIncorrectNotes();
                notesCompletedLabel.setText(completed + " / " + scoreCalculator.getTotalNotes());
            }

            // Update streak
            if (streakLabel != null) {
                streakLabel.setText("Streak: " + currentSession.getCurrentStreak());
            }

            // Update time
            if (timeLabel != null) {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                long seconds = elapsed / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;
                timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
            }

            // Update progress
            if (progressBar != null && isPlaying) {
                progressBar.setProgress(playbackController.getProgressPercentage() / 100.0);
            }

            // Update tempo label
            updateTempoLabel();
        });
    }

    private void updateTempoLabel() {
        if (tempoLabel != null && tempoSlider != null) {
            tempoLabel.setText(String.format("%.0f%%", tempoSlider.getValue()));
        }
    }

    private void processNoteDetection() {
        if (!isPlaying) return;

        // Get expected note from playback
        Note expectedNote = playbackController.getCurrentNote();
        if (expectedNote == null) return;

        // Get detected note from engine
        DetectedNote detectedNote = noteDetectionEngine.getLastDetectedNote();
        if (detectedNote == null) return;

        // Compare notes
        boolean correct = noteDetectionEngine.isNoteCorrect(detectedNote, expectedNote);

        if (correct) {
            // Correct note
            scoreCalculator.recordCorrectNote();
            currentSession.recordCorrectNote(detectedNote.getTimingDiff(expectedNote));
            showFeedback("✓ Correct!", Color.GREEN);
        } else {
            // Incorrect note
            scoreCalculator.recordIncorrectNote();
            currentSession.recordIncorrectNote();
            showFeedback("✗ Wrong note", Color.RED);
        }
    }

    private void showFeedback(String message, Color color) {
        Platform.runLater(() -> {
            if (feedbackLabel != null) {
                feedbackLabel.setText(message);
                feedbackLabel.setTextFill(color);
            }
        });
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        LOGGER.info(message);
    }

    private void disableControls() {
        if (playButton != null) playButton.setDisable(true);
        if (pauseButton != null) pauseButton.setDisable(true);
        if (stopButton != null) stopButton.setDisable(true);
        if (tempoSlider != null) tempoSlider.setDisable(true);
    }
}
