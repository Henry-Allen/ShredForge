package com.shredforge;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML private BorderPane mainContainer;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private VBox lessonsContainer;
    @FXML private ListView<String> recentSessionsList;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label progressLabel;
    @FXML private Button startPracticeButton;
    @FXML private Button tunerButton;
    @FXML private Button lessonsButton;
    @FXML private Button statsButton;

    private ObservableList<String> recentSessions;

    @FXML
    public void initialize() {
        setupDashboard();
        loadRecentSessions();
        updateTime();
        loadLessons();
    }

    private void setupDashboard() {
        welcomeLabel.setText("Welcome to ShredForge! 🎸");
        overallProgressBar.setProgress(0.35);
        progressLabel.setText("35% Complete - 12/35 Lessons");
    }

    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy - h:mm a");
        currentTimeLabel.setText(formatter.format(now));
    }

    private void loadRecentSessions() {
        recentSessions = FXCollections.observableArrayList(
            "Nov 3 - Power Chords Practice (25 min)",
            "Nov 2 - Alternate Picking (30 min)",
            "Nov 1 - Scale Practice - A Minor (20 min)",
            "Oct 31 - Chord Changes (15 min)",
            "Oct 30 - Finger Exercise (10 min)"
        );
        recentSessionsList.setItems(recentSessions);
    }

    private void loadLessons() {
        String[] lessonCategories = {
            "🎼 Beginner Fundamentals",
            "🎸 Intermediate Techniques", 
            "⚡ Advanced Shredding",
            "🎵 Music Theory"
        };

        for (String category : lessonCategories) {
            Label categoryLabel = new Label(category);
            categoryLabel.getStyleClass().add("lesson-category");
            lessonsContainer.getChildren().add(categoryLabel);
        }
    }

    @FXML
    private void handleStartPractice() {
        try {
            App.setRoot("practice");
        } catch (Exception e) {
            showError("Failed to start practice session: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenTuner() {
        try {
            App.setRoot("tuner");
        } catch (Exception e) {
            showError("Failed to open tuner: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenLessons() {
        try {
            App.setRoot("lessons");
        } catch (Exception e) {
            showError("Failed to open lessons: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenStats() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Practice Statistics");
        alert.setHeaderText("Your Progress This Week");
        alert.setContentText(
            "Total Practice Time: 2h 15min\n" +
            "Lessons Completed: 5\n" +
            "Current Streak: 4 days\n" +
            "Average Session: 22 minutes"
        );
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
