package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.Tab;
import com.shredforge.repository.ShredForgeRepository;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the main dashboard view
 * Displays user statistics, recent sessions, and quick access features
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    private Label totalTabsLabel;

    @FXML
    private Label totalPracticeTimeLabel;

    @FXML
    private Label averageScoreLabel;

    @FXML
    private Label currentStreakLabel;

    @FXML
    private VBox recentSessionsBox;

    @FXML
    private HBox achievementsBox;

    @FXML
    private Label skillLevelLabel;

    private ShredForgeRepository repository;

    @FXML
    public void initialize() {
        repository = ShredForgeRepository.getInstance();
        loadDashboardData();

        // Apply fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        if (totalTabsLabel != null && totalTabsLabel.getParent() != null) {
            fadeIn.setNode(totalTabsLabel.getParent());
            fadeIn.play();
        }
    }

    /**
     * Load dashboard statistics and data
     */
    private void loadDashboardData() {
        try {
            // Load total tabs count
            List<Tab> tabs = repository.getAllTabs();
            if (totalTabsLabel != null) {
                totalTabsLabel.setText(String.valueOf(tabs.size()));
            }

            // Load practice statistics
            loadPracticeStatistics();

            // Load recent sessions
            loadRecentSessions();

            // Load achievements
            loadAchievements();

            LOGGER.info("Dashboard data loaded successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard data", e);
            if (totalTabsLabel != null) {
                totalTabsLabel.setText("0");
            }
        }
    }

    /**
     * Load practice statistics from repository
     */
    private void loadPracticeStatistics() {
        // Default values for now - can be enhanced with repository methods
        if (totalPracticeTimeLabel != null) {
            totalPracticeTimeLabel.setText("0h 0m");
        }
        if (averageScoreLabel != null) {
            averageScoreLabel.setText("0.0%");
        }
        if (currentStreakLabel != null) {
            currentStreakLabel.setText("0 days");
        }
        if (skillLevelLabel != null) {
            skillLevelLabel.setText("Beginner");
        }
    }

    /**
     * Load recent practice sessions
     */
    private void loadRecentSessions() {
        if (recentSessionsBox == null) return;

        recentSessionsBox.getChildren().clear();

        Label placeholderLabel = new Label("No recent sessions yet");
        placeholderLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
        recentSessionsBox.getChildren().add(placeholderLabel);
    }

    /**
     * Load user achievements
     */
    private void loadAchievements() {
        if (achievementsBox == null) return;

        achievementsBox.getChildren().clear();

        Label placeholderLabel = new Label("Start practicing to earn achievements!");
        placeholderLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
        achievementsBox.getChildren().add(placeholderLabel);
    }

    /**
     * Navigate to practice view
     */
    @FXML
    private void handleStartPractice() {
        try {
            App.setRoot("practicesession");
            LOGGER.info("Navigating to practice session");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to practice session", e);
            DialogHelper.showError("Navigation Error", "Could not open practice session");
        }
    }

    /**
     * Navigate to browse tabs view
     */
    @FXML
    private void handleBrowseTabs() {
        try {
            App.setRoot("tabsearch");
            LOGGER.info("Navigating to tab search");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to tab search", e);
            DialogHelper.showError("Navigation Error", "Could not open tab browser");
        }
    }

    /**
     * Navigate to lessons view
     */
    @FXML
    private void handleViewLessons() {
        try {
            App.setRoot("lessons");
            LOGGER.info("Navigating to lessons");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to lessons", e);
            DialogHelper.showError("Navigation Error", "Could not open lessons");
        }
    }

    /**
     * Navigate to tuner
     */
    @FXML
    private void handleOpenTuner() {
        try {
            App.setRoot("tuner");
            LOGGER.info("Navigating to tuner");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to tuner", e);
            DialogHelper.showError("Navigation Error", "Could not open tuner");
        }
    }

    /**
     * Navigate to my tabs
     */
    @FXML
    private void handleMyTabs() {
        try {
            App.setRoot("mytabs");
            LOGGER.info("Navigating to my tabs");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to my tabs", e);
            DialogHelper.showError("Navigation Error", "Could not open my tabs");
        }
    }

    /**
     * Navigate to settings
     */
    @FXML
    private void handleSettings() {
        try {
            App.setRoot("settings");
            LOGGER.info("Navigating to settings");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to settings", e);
            DialogHelper.showError("Navigation Error", "Could not open settings");
        }
    }

    /**
     * Return to main menu
     */
    @FXML
    private void handleBackToMenu() {
        try {
            App.setRoot("mainmenu");
            LOGGER.info("Returning to main menu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main menu", e);
            DialogHelper.showError("Navigation Error", "Could not return to main menu");
        }
    }
}
