package com.shredforge.ui;

import com.shredforge.App;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the practice mode with metronome
 * Provides timing exercises and visual metronome
 */
public class PracticeController {

    private static final Logger LOGGER = Logger.getLogger(PracticeController.class.getName());

    @FXML
    private Canvas metronomeCanvas;

    @FXML
    private Slider tempoSlider;

    @FXML
    private Label tempoLabel;

    @FXML
    private ChoiceBox<String> timeSignatureChoice;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private VBox exercisesBox;

    @FXML
    private Label statusLabel;

    @FXML
    private CheckBox soundEnabledCheck;

    private boolean isPlaying = false;
    private int currentBeat = 0;
    private int beatsPerMeasure = 4;
    private long lastBeatTime = 0;
    private AnimationTimer metronomeTimer;

    @FXML
    public void initialize() {
        setupMetronome();
        setupExercises();

        // Apply fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        if (metronomeCanvas != null) {
            fadeIn.setNode(metronomeCanvas.getParent());
            fadeIn.play();
        }

        LOGGER.info("PracticeController initialized");
    }

    /**
     * Setup metronome controls and canvas
     */
    private void setupMetronome() {
        // Setup tempo slider
        if (tempoSlider != null) {
            tempoSlider.setValue(120);
            tempoSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateTempoLabel(newVal.intValue());
            });
            updateTempoLabel(120);
        }

        // Setup time signature selector
        if (timeSignatureChoice != null) {
            timeSignatureChoice.getItems().addAll("4/4", "3/4", "6/8", "5/4", "7/8");
            timeSignatureChoice.setValue("4/4");
            timeSignatureChoice.setOnAction(e -> updateTimeSignature());
        }

        // Setup sound checkbox
        if (soundEnabledCheck != null) {
            soundEnabledCheck.setSelected(true);
        }

        // Draw initial metronome state
        if (metronomeCanvas != null) {
            drawMetronome();
        }
    }

    /**
     * Setup practice exercises
     */
    private void setupExercises() {
        if (exercisesBox == null) return;

        List<Exercise> exercises = new ArrayList<>();
        exercises.add(new Exercise("Whole Notes", "Play one note per measure (4 beats)", "40-60 BPM", "⬤"));
        exercises.add(new Exercise("Half Notes", "Play notes on beats 1 and 3", "60-80 BPM", "⬤ ⬤"));
        exercises.add(new Exercise("Quarter Notes", "Play on every beat", "80-120 BPM", "⬤ ⬤ ⬤ ⬤"));
        exercises.add(new Exercise("Eighth Notes", "Play two notes per beat", "60-100 BPM", "⬤⬤ ⬤⬤ ⬤⬤ ⬤⬤"));
        exercises.add(new Exercise("Triplets", "Play three notes per beat", "60-90 BPM", "⬤⬤⬤ ⬤⬤⬤"));

        for (Exercise exercise : exercises) {
            exercisesBox.getChildren().add(createExerciseCard(exercise));
        }
    }

    /**
     * Create a visual card for an exercise
     */
    private VBox createExerciseCard(Exercise exercise) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(15, 52, 96, 0.3); " +
                      "-fx-padding: 15; " +
                      "-fx-background-radius: 10; " +
                      "-fx-border-color: rgba(0, 217, 255, 0.2); " +
                      "-fx-border-width: 1.5; " +
                      "-fx-border-radius: 10;");

        Label titleLabel = new Label(exercise.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        Label descLabel = new Label(exercise.getDescription());
        descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        Label tempoLabel = new Label("Tempo: " + exercise.getTempo());
        tempoLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 12px;");

        Label patternLabel = new Label(exercise.getPattern());
        patternLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 18px;");

        card.getChildren().addAll(titleLabel, descLabel, tempoLabel, patternLabel);

        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-cursor: hand;", "")));

        return card;
    }

    /**
     * Update tempo label
     */
    private void updateTempoLabel(int bpm) {
        if (tempoLabel != null) {
            tempoLabel.setText(bpm + " BPM");
        }
    }

    /**
     * Update time signature
     */
    private void updateTimeSignature() {
        if (timeSignatureChoice == null) return;

        String timeSignature = timeSignatureChoice.getValue();
        switch (timeSignature) {
            case "3/4":
                beatsPerMeasure = 3;
                break;
            case "6/8":
                beatsPerMeasure = 6;
                break;
            case "5/4":
                beatsPerMeasure = 5;
                break;
            case "7/8":
                beatsPerMeasure = 7;
                break;
            default:
                beatsPerMeasure = 4;
        }

        LOGGER.info("Time signature changed to: " + timeSignature);
        drawMetronome();
    }

    /**
     * Start metronome
     */
    @FXML
    private void handleStart() {
        if (isPlaying) return;

        isPlaying = true;
        currentBeat = 0;
        lastBeatTime = System.currentTimeMillis();

        if (startButton != null) startButton.setDisable(true);
        if (stopButton != null) stopButton.setDisable(false);
        if (statusLabel != null) statusLabel.setText("Playing...");

        startMetronomeTimer();

        LOGGER.info("Metronome started");
    }

    /**
     * Stop metronome
     */
    @FXML
    private void handleStop() {
        if (!isPlaying) return;

        isPlaying = false;
        currentBeat = 0;

        if (startButton != null) startButton.setDisable(false);
        if (stopButton != null) stopButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Stopped");

        if (metronomeTimer != null) {
            metronomeTimer.stop();
        }

        drawMetronome();

        LOGGER.info("Metronome stopped");
    }

    /**
     * Start the metronome animation timer
     */
    private void startMetronomeTimer() {
        if (metronomeTimer != null) {
            metronomeTimer.stop();
        }

        metronomeTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPlaying) {
                    stop();
                    return;
                }

                int bpm = (int) (tempoSlider != null ? tempoSlider.getValue() : 120);
                long beatInterval = 60000 / bpm; // milliseconds per beat

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBeatTime >= beatInterval) {
                    onBeat();
                    lastBeatTime = currentTime;
                }

                drawMetronome();
            }
        };

        metronomeTimer.start();
    }

    /**
     * Handle beat event
     */
    private void onBeat() {
        currentBeat = (currentBeat + 1) % beatsPerMeasure;

        // Play sound if enabled
        if (soundEnabledCheck != null && soundEnabledCheck.isSelected()) {
            playClickSound(currentBeat == 0);
        }

        LOGGER.fine("Beat: " + (currentBeat + 1) + " / " + beatsPerMeasure);
    }

    /**
     * Play metronome click sound
     */
    private void playClickSound(boolean isDownbeat) {
        try {
            // Generate a simple beep using SourceDataLine
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (AudioSystem.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                // Generate tone (higher pitch for downbeat)
                double frequency = isDownbeat ? 1000 : 800;
                int duration = 50; // milliseconds
                int samples = (int) (44100 * duration / 1000);
                byte[] buffer = new byte[samples * 2];

                for (int i = 0; i < samples; i++) {
                    double angle = 2.0 * Math.PI * i * frequency / 44100;
                    short sample = (short) (Math.sin(angle) * 32767 * 0.3); // 30% volume
                    buffer[i * 2] = (byte) (sample >> 8);
                    buffer[i * 2 + 1] = (byte) (sample & 0xFF);
                }

                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to play click sound", e);
        }
    }

    /**
     * Draw metronome visualization on canvas
     */
    private void drawMetronome() {
        if (metronomeCanvas == null) return;

        GraphicsContext gc = metronomeCanvas.getGraphicsContext2D();
        double width = metronomeCanvas.getWidth();
        double height = metronomeCanvas.getHeight();

        // Clear canvas
        gc.setFill(Color.rgb(10, 10, 10));
        gc.fillRect(0, 0, width, height);

        // Draw beat indicators
        double beatWidth = width / beatsPerMeasure;
        for (int i = 0; i < beatsPerMeasure; i++) {
            double x = i * beatWidth + beatWidth / 2;
            double radius = 30;

            // Highlight current beat
            if (isPlaying && i == currentBeat) {
                // Glowing effect for active beat
                gc.setFill(i == 0 ? Color.rgb(233, 69, 96, 0.5) : Color.rgb(0, 217, 255, 0.5));
                gc.fillOval(x - radius * 1.5, height / 2 - radius * 1.5, radius * 3, radius * 3);

                gc.setFill(i == 0 ? Color.rgb(233, 69, 96) : Color.rgb(0, 217, 255));
                gc.fillOval(x - radius, height / 2 - radius, radius * 2, radius * 2);
            } else {
                // Inactive beat
                gc.setFill(Color.rgb(60, 60, 60));
                gc.fillOval(x - radius, height / 2 - radius, radius * 2, radius * 2);

                gc.setStroke(i == 0 ? Color.rgb(233, 69, 96, 0.5) : Color.rgb(0, 217, 255, 0.5));
                gc.setLineWidth(2);
                gc.strokeOval(x - radius, height / 2 - radius, radius * 2, radius * 2);
            }

            // Draw beat number
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(i + 1), x - 5, height / 2 + 5);
        }
    }

    /**
     * Return to main menu
     */
    @FXML
    private void handleBackToMenu() {
        handleStop(); // Stop metronome before leaving

        try {
            App.setRoot("mainmenu");
            LOGGER.info("Returning to main menu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main menu", e);
            DialogHelper.showError("Navigation Error", "Could not return to main menu");
        }
    }

    /**
     * Inner class representing a practice exercise
     */
    private static class Exercise {
        private final String name;
        private final String description;
        private final String tempo;
        private final String pattern;

        public Exercise(String name, String description, String tempo, String pattern) {
            this.name = name;
            this.description = description;
            this.tempo = tempo;
            this.pattern = pattern;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTempo() {
            return tempo;
        }

        public String getPattern() {
            return pattern;
        }
    }
}
