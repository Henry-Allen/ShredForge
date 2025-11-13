package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.ScoreReport;
import com.shredforge.model.Session;
import com.shredforge.repository.ShredForgeRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.logging.Logger;

/**
 * Controller for Score Report interface.
 * Displays detailed performance analysis after practice session.
 * Per specification section 4.2.5
 */
public class ScoreReportController {
    private static final Logger LOGGER = Logger.getLogger(ScoreReportController.class.getName());

    @FXML
    private Label scoreLabel;

    @FXML
    private Label gradeLabel;

    @FXML
    private HBox starRatingBox;

    @FXML
    private Label totalNotesLabel;

    @FXML
    private Label correctNotesLabel;

    @FXML
    private Label incorrectNotesLabel;

    @FXML
    private Label missedNotesLabel;

    @FXML
    private Label avgTimingLabel;

    @FXML
    private Label longestStreakLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Label tabTitleLabel;

    @FXML
    private Button practiceAgainButton;

    @FXML
    private Button backToMenuButton;

    private final ShredForgeRepository repository;
    private Session completedSession;
    private ScoreReport scoreReport;

    public ScoreReportController() {
        this.repository = ShredForgeRepository.getInstance();
    }

    @FXML
    private void initialize() {
        LOGGER.info("Score Report initialized");

        // Get the last completed session
        completedSession = repository.getCurrentSession();

        if (completedSession == null) {
            LOGGER.warning("No session data available");
            showError();
            return;
        }

        // Generate score report
        scoreReport = completedSession.generateReport();

        // Display results
        displayReport();
    }

    private void displayReport() {
        if (scoreReport == null) return;

        Platform.runLater(() -> {
            // Tab title
            if (tabTitleLabel != null && completedSession != null) {
                tabTitleLabel.setText(completedSession.getTab().getTitle() + " - " +
                                    completedSession.getTab().getArtist());
            }

            // Overall Score
            if (scoreLabel != null) {
                float accuracy = scoreReport.getAccuracyPercentage();
                scoreLabel.setText(String.format("%.1f%%", accuracy));
                scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
            }

            // Grade
            if (gradeLabel != null) {
                String grade = scoreReport.getGrade();
                gradeLabel.setText(grade);
                gradeLabel.setStyle(getGradeStyle(grade));
            }

            // Star Rating
            if (starRatingBox != null) {
                displayStarRating(scoreReport.getStarRating());
            }

            // Statistics
            Session session = scoreReport.getSession();
            if (session != null) {
                if (totalNotesLabel != null) {
                    totalNotesLabel.setText(String.valueOf(session.getTotalNotes()));
                }

                if (correctNotesLabel != null) {
                    correctNotesLabel.setText(String.valueOf(session.getCorrectNotes()));
                    correctNotesLabel.setTextFill(Color.GREEN);
                }

                if (incorrectNotesLabel != null) {
                    incorrectNotesLabel.setText(String.valueOf(session.getIncorrectNotes()));
                    incorrectNotesLabel.setTextFill(Color.RED);
                }

                if (missedNotesLabel != null) {
                    missedNotesLabel.setText(String.valueOf(session.getMissedNotes()));
                    missedNotesLabel.setTextFill(Color.ORANGE);
                }

                if (avgTimingLabel != null) {
                    avgTimingLabel.setText(String.format("%.0f ms", scoreReport.getAverageTimingError()));
                }

                if (longestStreakLabel != null) {
                    longestStreakLabel.setText(String.valueOf(session.getLongestStreak()));
                }

                if (durationLabel != null) {
                    long duration = session.getDuration();
                    long seconds = duration / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    durationLabel.setText(String.format("%02d:%02d", minutes, seconds));
                }
            }
        });

        LOGGER.info("Score report displayed: " + scoreReport.getGrade() +
                   " (" + scoreReport.getAccuracyPercentage() + "%)");
    }

    private void displayStarRating(int stars) {
        if (starRatingBox == null) return;

        starRatingBox.getChildren().clear();

        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= stars ? "★" : "☆");
            star.setStyle("-fx-font-size: 36px; -fx-text-fill: " +
                         (i <= stars ? "#FFD700" : "#CCCCCC") + ";");
            starRatingBox.getChildren().add(star);
        }
    }

    private String getGradeStyle(String grade) {
        String color;
        switch (grade) {
            case "S":
                color = "#FFD700"; // Gold
                break;
            case "A":
                color = "#4CAF50"; // Green
                break;
            case "B":
                color = "#2196F3"; // Blue
                break;
            case "C":
                color = "#FF9800"; // Orange
                break;
            default:
                color = "#F44336"; // Red
        }

        return "-fx-font-size: 72px; -fx-font-weight: bold; -fx-text-fill: " + color + ";";
    }

    @FXML
    private void handlePracticeAgain() {
        LOGGER.info("Practice Again clicked");
        try {
            // Keep the same tab loaded
            App.setRoot("practicesession");
        } catch (Exception e) {
            LOGGER.severe("Failed to restart practice: " + e.getMessage());
            handleBackToMenu();
        }
    }

    @FXML
    private void handleBackToMenu() {
        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void showError() {
        if (scoreLabel != null) {
            scoreLabel.setText("No Data");
        }
        if (gradeLabel != null) {
            gradeLabel.setText("-");
        }
        if (practiceAgainButton != null) {
            practiceAgainButton.setDisable(true);
        }
    }
}
