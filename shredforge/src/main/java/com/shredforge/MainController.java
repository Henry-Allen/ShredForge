package com.shredforge;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainController - Enhanced Dashboard with Real-time Statistics
 * 
 * Features:
 * - Real-time clock with date display
 * - Practice statistics tracking (streak, time, accuracy)
 * - Recent session history with details
 * - Quick access navigation to all features
 * - Animated UI elements and smooth transitions
 * - Progress tracking and goal visualization
 * 
 * @version 1.1
 * @author Team 2 - ShredForge
 */
public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    // FXML Components
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

    // Data
    private ObservableList<String> recentSessions;
    private Timeline clockTimeline;
    private List<SessionData> sessionHistory;
    
    // Statistics
    private int currentStreak = 7;
    private double totalPracticeHours = 12.5;
    private int averageAccuracy = 87;
    private int lessonsCompleted = 5;
    private int totalLessons = 36;
    private double overallProgress = 0.35;
    
    // Constants
    private static final int MAX_RECENT_SESSIONS = 10;
    private static final String[] LESSON_CATEGORIES = {
        "🎼 Beginner Fundamentals",
        "🎸 Intermediate Techniques", 
        "⚡ Advanced Shredding",
        "🎵 Music Theory"
    };

    @FXML
    public void initialize() {
        LOGGER.info("Initializing MainController");
        try {
            initializeSessionHistory();
            setupDashboard();
            loadRecentSessions();
            updateTime();
            loadLessons();
            startClockUpdate();
            animateWelcome();
            LOGGER.info("Dashboard initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dashboard", e);
            showError("Initialization Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    /**
     * Initializes session history with sample data
     */
    private void initializeSessionHistory() {
        sessionHistory = new ArrayList<>();
        
        // Add sample session data
        sessionHistory.add(new SessionData(
            LocalDateTime.now().minusDays(0),
            "Power Chords Practice",
            25,
            4,
            92
        ));
        sessionHistory.add(new SessionData(
            LocalDateTime.now().minusDays(1),
            "Alternate Picking",
            30,
            5,
            88
        ));
        sessionHistory.add(new SessionData(
            LocalDateTime.now().minusDays(2),
            "Scale Practice - A Minor",
            20,
            3,
            85
        ));
        sessionHistory.add(new SessionData(
            LocalDateTime.now().minusDays(3),
            "Chord Changes",
            15,
            4,
            90
        ));
        sessionHistory.add(new SessionData(
            LocalDateTime.now().minusDays(4),
            "Finger Exercise",
            10,
            3,
            78
        ));
    }

    /**
     * Animates the welcome screen with fade and scale effects
     */
    private void animateWelcome() {
        // Fade in animation for main container
        FadeTransition fade = new FadeTransition(Duration.millis(800), mainContainer);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
        
        // Animate progress bar fill
        Timeline progressAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(overallProgressBar.progressProperty(), 0.0)),
            new KeyFrame(Duration.millis(1500), new KeyValue(overallProgressBar.progressProperty(), overallProgress))
        );
        progressAnimation.play();
        
        // Stagger animate buttons with delays
        animateButton(startPracticeButton, 200);
        animateButton(lessonsButton, 400);
        animateButton(tunerButton, 600);
        animateButton(statsButton, 800);
    }

    /**
     * Animates a button with slide and fade effect
     */
    private void animateButton(Button button, int delayMs) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), button);
        slide.setFromY(20);
        slide.setToY(0);
        slide.setDelay(Duration.millis(delayMs));
        
        FadeTransition fade = new FadeTransition(Duration.millis(400), button);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(delayMs));
        
        ParallelTransition parallel = new ParallelTransition(slide, fade);
        parallel.play();
    }

    /**
     * Sets up the dashboard with welcome message and stats
     */
    private void setupDashboard() {
        String userName = getUserName();
        welcomeLabel.setText("Welcome back, " + userName + "! 🎸");
        animateLabel(welcomeLabel);
        
        // Update progress label
        int percentComplete = (int) (overallProgress * 100);
        progressLabel.setText(percentComplete + "% Complete");
        
        LOGGER.info("Dashboard setup complete for user: " + userName);
    }

    /**
     * Gets the user name (can be loaded from preferences)
     */
    private String getUserName() {
        // TODO: Load from user preferences/settings
        return "Guitarist";
    }

    /**
     * Starts the clock update timer
     */
    private void startClockUpdate() {
        clockTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> updateTime())
        );
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Updates the current time display
     */
    private void updateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy - h:mm a");
        currentTimeLabel.setText(formatter.format(now));
    }

    /**
     * Loads recent practice sessions into the list view
     */
    private void loadRecentSessions() {
        recentSessions = FXCollections.observableArrayList();
        
        // Format session history for display
        for (SessionData session : sessionHistory) {
            String formattedSession = formatSessionForDisplay(session);
            recentSessions.add(formattedSession);
        }
        
        recentSessionsList.setItems(recentSessions);
        
        // Custom cell factory for better styling
        recentSessionsList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-padding: 12;");
                    
                    // Add hover effect
                    setOnMouseEntered(e -> setStyle("-fx-text-fill: white; -fx-background-color: rgba(0, 217, 255, 0.15); -fx-padding: 12; -fx-cursor: hand;"));
                    setOnMouseExited(e -> setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-padding: 12;"));
                }
            }
        });
        
        // Add click handler to view session details
        recentSessionsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = recentSessionsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showSessionDetails(selected);
                }
            }
        });
        
        LOGGER.info("Loaded " + recentSessions.size() + " recent sessions");
    }

    /**
     * Formats a session for display in the list
     */
    private String formatSessionForDisplay(SessionData session) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");
        String date = session.dateTime.format(dateFormatter);
        String stars = getStarRating(session.rating);
        return String.format("📅 %s - %s (%d min) %s", 
            date, session.name, session.durationMinutes, stars);
    }

    /**
     * Gets star rating string based on numeric rating
     */
    private String getStarRating(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }

    /**
     * Shows detailed information about a session
     */
    private void showSessionDetails(String sessionText) {
        // Find the corresponding session data
        for (SessionData session : sessionHistory) {
            if (sessionText.contains(session.name)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Session Details");
                alert.setHeaderText(session.name);
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
                alert.setContentText(
                    "📅 Date: " + session.dateTime.format(formatter) + "\n" +
                    "⏱️ Duration: " + session.durationMinutes + " minutes\n" +
                    "⭐ Rating: " + session.rating + "/5\n" +
                    "🎯 Accuracy: " + session.accuracy + "%\n\n" +
                    "Keep up the great work!"
                );
                alert.showAndWait();
                break;
            }
        }
    }

    /**
     * Loads lesson categories with animation
     */
    private void loadLessons() {
        for (int i = 0; i < LESSON_CATEGORIES.length; i++) {
            String category = LESSON_CATEGORIES[i];
            Label categoryLabel = new Label(category);
            categoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0;");
            categoryLabel.setOpacity(0);
            
            // Make clickable
            categoryLabel.setOnMouseEntered(e -> categoryLabel.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0; -fx-cursor: hand;"));
            categoryLabel.setOnMouseExited(e -> categoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0;"));
            categoryLabel.setOnMouseClicked(e -> handleOpenLessons());
            
            lessonsContainer.getChildren().add(categoryLabel);
            
            // Stagger fade in animation
            FadeTransition fade = new FadeTransition(Duration.millis(400), categoryLabel);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(1000 + i * 150));
            fade.play();
        }
    }

    @FXML
    private void handleStartPractice() {
        pulseButton(startPracticeButton);
        try {
            LOGGER.info("Navigating to practice mode");
            App.setRoot("practice");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start practice", e);
            showError("Navigation Error", "Failed to start practice session: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenTuner() {
        pulseButton(tunerButton);
        try {
            LOGGER.info("Navigating to tuner");
            App.setRoot("tuner");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open tuner", e);
            showError("Navigation Error", "Failed to open tuner: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenLessons() {
        pulseButton(lessonsButton);
        try {
            LOGGER.info("Navigating to lessons");
            App.setRoot("lessons");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open lessons", e);
            showError("Navigation Error", "Failed to open lessons: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenStats() {
        pulseButton(statsButton);
        showDetailedStats();
    }

    /**
     * Shows detailed statistics dialog with comprehensive information
     */
    private void showDetailedStats() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Practice Statistics");
        alert.setHeaderText("Your Progress This Week 📊");
        
        // Calculate weekly statistics
        int weeklyPracticeMinutes = (int) (totalPracticeHours * 60);
        int weeklyPracticeHours = weeklyPracticeMinutes / 60;
        int weeklyPracticeRemainingMinutes = weeklyPracticeMinutes % 60;
        double dailyAverage = totalPracticeHours / 7;
        int totalSessions = sessionHistory.size();
        int averageSessionLength = totalSessions > 0 ? weeklyPracticeMinutes / totalSessions : 0;
        
        alert.setContentText(
            "🔥 Current Streak: " + currentStreak + " days\n" +
            "⏱️ Total Practice Time: " + weeklyPracticeHours + "h " + weeklyPracticeRemainingMinutes + "m\n" +
            "📅 Daily Average: " + String.format("%.1f", dailyAverage) + " hours\n" +
            "📚 Lessons Completed: " + lessonsCompleted + " / " + totalLessons + "\n" +
            "🎯 Average Accuracy: " + averageAccuracy + "%\n" +
            "⭐ Total Sessions: " + totalSessions + "\n" +
            "⏲️ Average Session: " + averageSessionLength + " minutes\n" +
            "📈 Overall Progress: " + (int)(overallProgress * 100) + "%\n\n" +
            getMotivationalMessage()
        );
        
        alert.showAndWait();
        LOGGER.info("Displayed detailed statistics");
    }

    /**
     * Gets motivational message based on performance
     */
    private String getMotivationalMessage() {
        if (currentStreak >= 7) {
            return "🎉 Amazing! You've practiced every day this week! Keep it up!";
        } else if (currentStreak >= 3) {
            return "💪 Great consistency! You're building a solid habit!";
        } else if (averageAccuracy >= 85) {
            return "🎯 Excellent accuracy! Your technique is improving!";
        } else if (totalPracticeHours >= 10) {
            return "⏱️ Great dedication! You're putting in the time!";
        } else {
            return "🎸 Keep practicing! Every session makes you better!";
        }
    }

    /**
     * Pulses a button for visual feedback
     */
    private void pulseButton(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    /**
     * Animates a label with scale effect
     */
    private void animateLabel(Label label) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(800), label);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        FadeTransition fade = new FadeTransition(Duration.millis(800), label);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        
        ParallelTransition parallel = new ParallelTransition(scale, fade);
        parallel.play();
    }

    /**
     * Shows an error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Cleanup method called when controller is destroyed
     */
    public void cleanup() {
        LOGGER.info("Cleaning up MainController");
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }

    /**
     * Inner class representing a practice session
     */
    private static class SessionData {
        LocalDateTime dateTime;
        String name;
        int durationMinutes;
        int rating; // 1-5 stars
        int accuracy; // 0-100%
        
        SessionData(LocalDateTime dateTime, String name, int durationMinutes, int rating, int accuracy) {
            this.dateTime = dateTime;
            this.name = name;
            this.durationMinutes = durationMinutes;
            this.rating = rating;
            this.accuracy = accuracy;
        }
    }

    /**
     * Saves current session to history
     */
    public void addSession(String name, int durationMinutes, int rating, int accuracy) {
        SessionData newSession = new SessionData(
            LocalDateTime.now(),
            name,
            durationMinutes,
            rating,
            accuracy
        );
        
        sessionHistory.add(0, newSession); // Add to beginning
        
        // Keep only recent sessions
        if (sessionHistory.size() > MAX_RECENT_SESSIONS) {
            sessionHistory.remove(sessionHistory.size() - 1);
        }
        
        // Reload the display
        loadRecentSessions();
        
        // Update statistics
        updateStatistics();
        
        LOGGER.info("Added new session: " + name);
    }

    /**
     * Updates overall statistics based on session history
     */
    private void updateStatistics() {
        if (sessionHistory.isEmpty()) {
            return;
        }
        
        // Calculate total practice time
        int totalMinutes = 0;
        int totalAccuracySum = 0;
        
        for (SessionData session : sessionHistory) {
            totalMinutes += session.durationMinutes;
            totalAccuracySum += session.accuracy;
        }
        
        totalPracticeHours = totalMinutes / 60.0;
        averageAccuracy = totalAccuracySum / sessionHistory.size();
        
        // Update streak (check consecutive days)
        currentStreak = calculateStreak();
        
        LOGGER.info("Statistics updated: " + totalPracticeHours + "h, " + 
                   averageAccuracy + "% accuracy, " + currentStreak + " day streak");
    }

    /**
     * Calculates current practice streak
     */
    private int calculateStreak() {
        if (sessionHistory.isEmpty()) {
            return 0;
        }
        
        int streak = 1;
        LocalDateTime lastDate = sessionHistory.get(0).dateTime.toLocalDate().atStartOfDay();
        
        for (int i = 1; i < sessionHistory.size(); i++) {
            LocalDateTime currentDate = sessionHistory.get(i).dateTime.toLocalDate().atStartOfDay();
            long daysDiff = java.time.Duration.between(currentDate, lastDate).toDays();
            
            if (daysDiff == 1) {
                streak++;
                lastDate = currentDate;
            } else {
                break;
            }
        }
        
        return streak;
    }

    /**
     * Updates progress based on completed lessons
     */
    public void updateProgress(int completedLessons) {
        this.lessonsCompleted = completedLessons;
        this.overallProgress = (double) completedLessons / totalLessons;
        
        // Animate progress bar
        Timeline progressAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(overallProgressBar.progressProperty(), 
                    overallProgressBar.getProgress())),
            new KeyFrame(Duration.millis(1000), 
                new KeyValue(overallProgressBar.progressProperty(), 
                    overallProgress))
        );
        progressAnimation.play();
        
        // Update label
        int percentComplete = (int) (overallProgress * 100);
        progressLabel.setText(percentComplete + "% Complete");
        
        LOGGER.info("Progress updated: " + completedLessons + " lessons completed");
    }
}