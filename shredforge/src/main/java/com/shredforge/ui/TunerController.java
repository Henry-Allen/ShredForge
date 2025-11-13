package com.shredforge.ui;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.shredforge.App;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for guitar tuner with pitch detection
 * Uses TarsosDSP YIN algorithm for accurate pitch detection
 */
public class TunerController {

    private static final Logger LOGGER = Logger.getLogger(TunerController.class.getName());

    // Standard guitar tuning (Hz)
    private static final Map<String, Double> STANDARD_TUNING = new LinkedHashMap<>();
    static {
        STANDARD_TUNING.put("E2", 82.41);   // Low E string
        STANDARD_TUNING.put("A2", 110.00);  // A string
        STANDARD_TUNING.put("D3", 146.83);  // D string
        STANDARD_TUNING.put("G3", 196.00);  // G string
        STANDARD_TUNING.put("B3", 246.94);  // B string
        STANDARD_TUNING.put("E4", 329.63);  // High E string
    }

    @FXML
    private Canvas tunerCanvas;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Label frequencyLabel;

    @FXML
    private Label noteLabel;

    @FXML
    private Label centsLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar tuningBar;

    @FXML
    private Label stringLabel;

    private boolean isRunning = false;
    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private double currentFrequency = 0.0;
    private String detectedNote = "";
    private double centsDiff = 0.0;

    @FXML
    public void initialize() {
        setupUI();

        // Apply fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        if (tunerCanvas != null) {
            fadeIn.setNode(tunerCanvas.getParent());
            fadeIn.play();
        }

        // Draw initial tuner state
        if (tunerCanvas != null) {
            drawTuner(0.0);
        }

        LOGGER.info("TunerController initialized");
    }

    /**
     * Setup UI controls
     */
    private void setupUI() {
        if (stopButton != null) {
            stopButton.setDisable(true);
        }

        if (tuningBar != null) {
            tuningBar.setProgress(0.5); // Center position
        }

        if (statusLabel != null) {
            statusLabel.setText("Ready");
        }

        if (noteLabel != null) {
            noteLabel.setText("-");
        }

        if (frequencyLabel != null) {
            frequencyLabel.setText("0.0 Hz");
        }

        if (centsLabel != null) {
            centsLabel.setText("0¢");
        }

        if (stringLabel != null) {
            stringLabel.setText("Play a string");
        }
    }

    /**
     * Start the tuner
     */
    @FXML
    private void handleStart() {
        if (isRunning) return;

        try {
            startTuner();

            if (startButton != null) startButton.setDisable(true);
            if (stopButton != null) stopButton.setDisable(false);
            if (statusLabel != null) statusLabel.setText("Listening...");

            LOGGER.info("Tuner started");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start tuner", e);
            DialogHelper.showError("Tuner Error", "Could not access microphone: " + e.getMessage());
        }
    }

    /**
     * Stop the tuner
     */
    @FXML
    private void handleStop() {
        if (!isRunning) return;

        stopTuner();

        if (startButton != null) startButton.setDisable(false);
        if (stopButton != null) stopButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Stopped");

        // Reset display
        Platform.runLater(() -> {
            if (noteLabel != null) noteLabel.setText("-");
            if (frequencyLabel != null) frequencyLabel.setText("0.0 Hz");
            if (centsLabel != null) centsLabel.setText("0¢");
            if (stringLabel != null) stringLabel.setText("Play a string");
            if (tuningBar != null) tuningBar.setProgress(0.5);
            if (tunerCanvas != null) drawTuner(0.0);
        });

        LOGGER.info("Tuner stopped");
    }

    /**
     * Start pitch detection
     */
    private void startTuner() throws LineUnavailableException {
        isRunning = true;

        // Setup audio format
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio line not supported");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format, 4096);
        line.start();

        // Create audio dispatcher
        JVMAudioInputStream audioStream = new JVMAudioInputStream(new AudioInputStream(line));
        dispatcher = new AudioDispatcher(audioStream, 2048, 1024);

        // Setup pitch detection handler
        PitchDetectionHandler pitchHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent event) {
                double pitchInHz = result.getPitch();

                if (pitchInHz != -1 && pitchInHz > 50 && pitchInHz < 1000) {
                    currentFrequency = pitchInHz;

                    Platform.runLater(() -> {
                        updateTunerDisplay(pitchInHz);
                    });
                }
            }
        };

        // Add pitch processor (YIN algorithm)
        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN,
                44100, 2048, pitchHandler));

        // Run in separate thread
        audioThread = new Thread(() -> {
            try {
                dispatcher.run();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in audio processing", e);
            }
        }, "Audio-Tuner-Thread");
        audioThread.start();
    }

    /**
     * Stop pitch detection
     */
    private void stopTuner() {
        isRunning = false;

        if (dispatcher != null) {
            dispatcher.stop();
        }

        if (audioThread != null) {
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while stopping tuner", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Update tuner display with detected frequency
     */
    private void updateTunerDisplay(double frequency) {
        // Find closest note
        NoteInfo noteInfo = findClosestNote(frequency);
        detectedNote = noteInfo.note;
        centsDiff = noteInfo.cents;

        // Update labels
        if (frequencyLabel != null) {
            frequencyLabel.setText(String.format("%.1f Hz", frequency));
        }

        if (noteLabel != null) {
            noteLabel.setText(detectedNote);
            noteLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + getNoteColor(centsDiff) + ";");
        }

        if (centsLabel != null) {
            String sign = centsDiff > 0 ? "+" : "";
            centsLabel.setText(sign + String.format("%.0f¢", centsDiff));
            centsLabel.setStyle("-fx-text-fill: " + getCentsColor(centsDiff) + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        }

        // Check if it matches a guitar string
        String matchedString = findMatchingString(detectedNote);
        if (stringLabel != null) {
            stringLabel.setText(matchedString != null ? "String: " + matchedString : "");
        }

        // Update tuning bar (0.0 = -50 cents, 0.5 = 0 cents, 1.0 = +50 cents)
        if (tuningBar != null) {
            double progress = 0.5 + (centsDiff / 100.0);
            progress = Math.max(0.0, Math.min(1.0, progress));
            tuningBar.setProgress(progress);
        }

        // Draw tuner visualization
        if (tunerCanvas != null) {
            drawTuner(centsDiff);
        }
    }

    /**
     * Find the closest musical note to a frequency
     */
    private NoteInfo findClosestNote(double frequency) {
        // A4 = 440 Hz
        double a4 = 440.0;
        double halfStepsFromA4 = 12 * Math.log(frequency / a4) / Math.log(2);
        int nearestHalfStep = (int) Math.round(halfStepsFromA4);

        // Calculate the actual frequency of that note
        double nearestFreq = a4 * Math.pow(2, nearestHalfStep / 12.0);

        // Calculate cents difference
        double cents = 1200 * Math.log(frequency / nearestFreq) / Math.log(2);

        // Get note name
        String[] noteNames = {"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"};
        int noteIndex = ((nearestHalfStep % 12) + 12) % 12;
        int octave = 4 + (nearestHalfStep + 9) / 12;

        String noteName = noteNames[noteIndex] + octave;

        return new NoteInfo(noteName, cents);
    }

    /**
     * Find which guitar string matches the detected note
     */
    private String findMatchingString(String note) {
        for (Map.Entry<String, Double> entry : STANDARD_TUNING.entrySet()) {
            String tuningNote = entry.getKey();
            if (tuningNote.startsWith(note.substring(0, Math.min(2, note.length())))) {
                int stringNum = new ArrayList<>(STANDARD_TUNING.keySet()).indexOf(tuningNote) + 1;
                return String.valueOf(stringNum) + " (" + tuningNote + ")";
            }
        }
        return null;
    }

    /**
     * Get color for note display based on tuning accuracy
     */
    private String getNoteColor(double cents) {
        if (Math.abs(cents) < 5) {
            return "#00ff88"; // Green - in tune
        } else if (Math.abs(cents) < 15) {
            return "#ffaa00"; // Orange - close
        } else {
            return "#00d9ff"; // Cyan - needs adjustment
        }
    }

    /**
     * Get color for cents display
     */
    private String getCentsColor(double cents) {
        if (Math.abs(cents) < 5) {
            return "#00ff88"; // Green
        } else if (cents > 0) {
            return "#e94560"; // Red/pink - sharp
        } else {
            return "#00d9ff"; // Cyan - flat
        }
    }

    /**
     * Draw tuner visualization on canvas
     */
    private void drawTuner(double cents) {
        if (tunerCanvas == null) return;

        GraphicsContext gc = tunerCanvas.getGraphicsContext2D();
        double width = tunerCanvas.getWidth();
        double height = tunerCanvas.getHeight();

        // Clear canvas
        gc.setFill(Color.rgb(10, 10, 10));
        gc.fillRect(0, 0, width, height);

        // Draw center line (in tune)
        double centerX = width / 2;
        gc.setStroke(Color.rgb(136, 136, 136));
        gc.setLineWidth(2);
        gc.strokeLine(centerX, 20, centerX, height - 20);

        // Draw tuning zones
        double zoneWidth = width / 4;

        // Sharp zone (red)
        gc.setFill(Color.rgb(233, 69, 96, 0.2));
        gc.fillRect(centerX, 0, zoneWidth, height);

        // Flat zone (cyan)
        gc.setFill(Color.rgb(0, 217, 255, 0.2));
        gc.fillRect(centerX - zoneWidth, 0, zoneWidth, height);

        // In-tune zone (green)
        gc.setFill(Color.rgb(0, 255, 136, 0.2));
        gc.fillRect(centerX - 20, 0, 40, height);

        // Draw indicator needle
        double centsScale = Math.max(-50, Math.min(50, cents)); // Clamp to -50/+50
        double needleX = centerX + (centsScale / 50.0) * zoneWidth;

        gc.setFill(Math.abs(cents) < 5 ? Color.rgb(0, 255, 136) : Color.rgb(233, 69, 96));
        gc.fillPolygon(
                new double[]{needleX - 10, needleX + 10, needleX},
                new double[]{20, 20, height - 20},
                3
        );

        // Draw labels
        gc.setFill(Color.WHITE);
        gc.fillText("FLAT", 10, height - 10);
        gc.fillText("IN TUNE", centerX - 30, height - 10);
        gc.fillText("SHARP", width - 50, height - 10);
    }

    /**
     * Return to main menu
     */
    @FXML
    private void handleBackToMenu() {
        handleStop(); // Stop tuner before leaving

        try {
            App.setRoot("mainmenu");
            LOGGER.info("Returning to main menu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main menu", e);
            DialogHelper.showError("Navigation Error", "Could not return to main menu");
        }
    }

    /**
     * Inner class to hold note information
     */
    private static class NoteInfo {
        final String note;
        final double cents;

        NoteInfo(String note, double cents) {
            this.note = note;
            this.cents = cents;
        }
    }
}
