package com.shredforge;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PracticeController {

    @FXML private Label timerLabel;
    @FXML private Label exerciseLabel;
    @FXML private TextArea instructionsArea;
    @FXML private ProgressBar sessionProgress;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button nextButton;
    @FXML private Button backToMainButton;
    @FXML private Slider tempoSlider;
    @FXML private Label tempoLabel;
    @FXML private VBox metronomeVisual;

    private Timeline timer;
    private int secondsElapsed = 0;
    private boolean isPaused = false;
    private int currentExercise = 0;

    private static final String[] EXERCISES = {
        "Finger Warmup - Chromatic Exercise",
        "Open Chord Practice - G, C, D",
        "Alternate Picking - E Minor Scale",
        "Power Chords - Root 5th Pattern",
        "String Skipping Exercise"
    };

    private static final String[] INSTRUCTIONS = {
        "Play 1-2-3-4 on each string, ascending and descending.\n" +
        "Start slowly and focus on clean notes.\n" +
        "Keep your fingers close to the fretboard.",
        
        "Practice smooth transitions between G, C, and D chords.\n" +
        "Strum each chord 4 times before switching.\n" +
        "Ensure all strings ring clearly.",
        
        "Play the E minor scale (E F# G A B C D E).\n" +
        "Use strict alternate picking: down-up-down-up.\n" +
        "Start at 60 BPM and increase gradually.",
        
        "Play power chords (root + 5th) on frets 3, 5, 7.\n" +
        "Mute unused strings with your palm.\n" +
        "Focus on clean, punchy sound.",
        
        "Skip strings while maintaining rhythm.\n" +
        "Play: Low E - D - G - High E pattern.\n" +
        "Use alternate picking throughout."
    };

    @FXML
    public void initialize() {
        pauseButton.setDisable(true);
        nextButton.setDisable(true);
        sessionProgress.setProgress(0);
        
        tempoSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tempoLabel.setText(newVal.intValue() + " BPM");
        });
        
        loadExercise(0);
    }

    private void loadExercise(int index) {
        if (index >= 0 && index < EXERCISES.length) {
            currentExercise = index;
            exerciseLabel.setText(EXERCISES[index]);
            instructionsArea.setText(INSTRUCTIONS[index]);
            sessionProgress.setProgress((double) index / EXERCISES.length);
        }
    }

    @FXML
    private void handleStart() {
        if (!isPaused) {
            secondsElapsed = 0;
        }
        
        startTimer();
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        nextButton.setDisable(false);
        isPaused = false;
    }

    @FXML
    private void handlePause() {
        if (timer != null) {
            timer.stop();
        }
        startButton.setText("Resume");
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        isPaused = true;
    }

    @FXML
    private void handleNext() {
        if (currentExercise < EXERCISES.length - 1) {
            loadExercise(currentExercise + 1);
            secondsElapsed = 0;
            updateTimerDisplay();
        } else {
            completePractice();
        }
    }

    @FXML
    private void handleBackToMain() {
        if (timer != null) {
            timer.stop();
        }
        
        try {
            App.setRoot("main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsElapsed++;
            updateTimerDisplay();
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerDisplay() {
        int minutes = secondsElapsed / 60;
        int seconds = secondsElapsed % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void completePractice() {
        if (timer != null) {
            timer.stop();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Practice Complete!");
        alert.setHeaderText("Great job! 🎸");
        alert.setContentText(
            "Session Duration: " + formatTime(secondsElapsed) + "\n" +
            "Exercises Completed: " + EXERCISES.length + "\n\n" +
            "Keep up the great work!"
        );
        
        alert.showAndWait();
        
        try {
            App.setRoot("main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d min %d sec", minutes, seconds);
    }
}
