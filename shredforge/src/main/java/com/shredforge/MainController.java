package com.shredforge;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

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
    private Timeline clockTimeline;
    private Timeline statsUpdateTimeline;
    private final List<SessionData> sessionHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<Achievement> achievements = new ArrayList<>();
    
    private volatile int currentStreak = 7;
    private volatile double totalPracticeHours = 12.5;
    private volatile int averageAccuracy = 87;
    private volatile int lessonsCompleted = 5;
    private volatile int totalLessons = 36;
    private volatile double overallProgress = 0.35;
    private volatile int totalSessions = 0;
    private volatile int longestStreak = 10;
    private volatile double weeklyGoalHours = 5.0;
    private volatile int perfectSessions = 0;
    
    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<DayOfWeek, Integer> practiceByDay = new EnumMap<>(DayOfWeek.class);
    private final Map<LocalDate, SessionData> sessionsByDate = new TreeMap<>();
    private final AtomicInteger totalNotesPlayed = new AtomicInteger(0);
    
    private static final int[] MILESTONE_HOURS = {5, 10, 25, 50, 100, 250, 500, 1000};
    private static final int[] MILESTONE_SESSIONS = {10, 25, 50, 100, 250, 500};
    
    private static final int MAX_RECENT_SESSIONS = 10;
    private static final String[] LESSON_CATEGORIES = {
        "🎼 Beginner Fundamentals",
        "🎸 Intermediate Techniques", 
        "⚡ Advanced Shredding",
        "🎵 Music Theory",
        "🎶 Songs & Repertoire",
        "🤘 Speed Techniques"
    };
    
    private static final String[] SKILL_CATEGORIES = {
        "Rhythm", "Lead", "Chords", "Scales", "Technique", "Theory"
    };

    @FXML
    public void initialize() {
        LOGGER.info("Initializing MainController");
        try {
            initializeSkillLevels();
            initializeSessionHistory();
            initializeAchievements();
            setupDashboard();
            loadRecentSessions();
            updateTime();
            
            // Check if lessonsContainer exists before trying to load lessons
            if (lessonsContainer != null) {
                loadLessons();
            } else {
                LOGGER.warning("lessonsContainer is null, skipping lesson loading");
            }
            
            startClockUpdate();
            startPeriodicStatsUpdate();
            animateWelcome();
            checkForMilestones();
            
            LOGGER.info("Dashboard initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dashboard", e);
            showError("Initialization Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    private void initializeSkillLevels() {
        for (String skill : SKILL_CATEGORIES) {
            skillLevels.put(skill, (int) (Math.random() * 100));
        }
        
        for (DayOfWeek day : DayOfWeek.values()) {
            practiceByDay.put(day, 0);
        }
    }

    private void initializeSessionHistory() {
        LocalDateTime now = LocalDateTime.now();
        
        String[] sessionTypes = {
            "Power Chords Practice", "Alternate Picking", "Scale Practice - A Minor",
            "Chord Changes", "Finger Exercise", "Sweep Picking", "Legato Runs",
            "Arpeggios", "Rhythm Training", "Solo Practice"
        };
        
        for (int i = 0; i < 14; i++) {
            LocalDateTime sessionDate = now.minusDays(i);
            String sessionName = sessionTypes[(int) (Math.random() * sessionTypes.length)];
            int duration = 10 + (int) (Math.random() * 40);
            int rating = 3 + (int) (Math.random() * 3);
            int accuracy = 70 + (int) (Math.random() * 30);
            int notesPlayed = duration * (10 + (int) (Math.random() * 20));
            
            SessionData session = new SessionData(
                sessionDate, sessionName, duration, rating, accuracy, notesPlayed
            );
            
            sessionHistory.add(session);
            sessionsByDate.put(sessionDate.toLocalDate(), session);
            
            DayOfWeek day = sessionDate.getDayOfWeek();
            practiceByDay.merge(day, 1, Integer::sum);
            
            totalNotesPlayed.addAndGet(notesPlayed);
        }
        
        sessionHistory.sort((a, b) -> b.dateTime.compareTo(a.dateTime));
        
        totalSessions = sessionHistory.size();
        LOGGER.info("Initialized " + sessionHistory.size() + " sessions");
    }

    private void initializeAchievements() {
        achievements.add(new Achievement("First Steps", "Complete your first practice session", false));
        achievements.add(new Achievement("Week Warrior", "Maintain a 7-day streak", currentStreak >= 7));
        achievements.add(new Achievement("Dedicated", "Practice 10 hours total", totalPracticeHours >= 10));
        achievements.add(new Achievement("Century Club", "Complete 100 sessions", totalSessions >= 100));
        achievements.add(new Achievement("Perfect Practice", "Get 95%+ accuracy in 10 sessions", perfectSessions >= 10));
    }

    private void checkForMilestones() {
        for (int milestone : MILESTONE_HOURS) {
            if (totalPracticeHours >= milestone && totalPracticeHours < milestone + 1) {
                LOGGER.info("Milestone reached: " + milestone + " hours");
                break;
            }
        }
        
        for (int milestone : MILESTONE_SESSIONS) {
            if (totalSessions >= milestone && totalSessions < milestone + 10) {
                LOGGER.info("Milestone reached: " + milestone + " sessions");
                break;
            }
        }
    }

    private void setupDashboard() {
        if (progressLabel != null) {
            progressLabel.setText(String.format("%d%% Complete - %d/%d Lessons", 
                (int)(overallProgress * 100), lessonsCompleted, totalLessons));
        }
        
        if (overallProgressBar != null) {
            overallProgressBar.setProgress(overallProgress);
        }
    }

    private void loadRecentSessions() {
        recentSessions = FXCollections.observableArrayList();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        
        for (int i = 0; i < Math.min(MAX_RECENT_SESSIONS, sessionHistory.size()); i++) {
            SessionData session = sessionHistory.get(i);
            String formattedDate = session.dateTime.format(formatter);
            String stars = "⭐".repeat(session.rating);
            recentSessions.add(String.format("%s - %s %s (%d min, %d%%)", 
                formattedDate, session.name, stars, session.durationMinutes, session.accuracy));
        }
        
        if (recentSessionsList != null) {
            recentSessionsList.setItems(recentSessions);
        }
    }

    private void updateTime() {
        if (currentTimeLabel != null) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd • hh:mm a");
            currentTimeLabel.setText(now.format(formatter));
        }
    }

    private void loadLessons() {
        if (lessonsContainer == null) {
            LOGGER.warning("lessonsContainer is null, cannot load lessons");
            return;
        }
        
        lessonsContainer.getChildren().clear();
        
        for (int i = 0; i < LESSON_CATEGORIES.length; i++) {
            String category = LESSON_CATEGORIES[i];
            HBox categoryBox = createCategoryBox(category, i);
            
            lessonsContainer.getChildren().add(categoryBox);
            
            categoryBox.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(400), categoryBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(100 + i * 100));
            fade.play();
        }
    }

    private HBox createCategoryBox(String category, int index) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 0, 8, 0));
        box.setStyle("-fx-cursor: hand;");
        
        Label categoryLabel = new Label(category);
        categoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        ProgressBar miniProgress = new ProgressBar(Math.random());
        miniProgress.setPrefWidth(80);
        miniProgress.setPrefHeight(6);
        miniProgress.setStyle("-fx-accent: linear-gradient(to right, #00ff88, #00d9ff);");
        
        box.getChildren().addAll(categoryLabel, miniProgress);
        
        box.setOnMouseEntered(e -> {
            categoryLabel.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 14px; -fx-font-weight: bold;");
        });
        
        box.setOnMouseExited(e -> {
            categoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        });
        
        return box;
    }

    private void startClockUpdate() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTime()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void startPeriodicStatsUpdate() {
        statsUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            LOGGER.fine("Periodic stats update");
        }));
        statsUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        statsUpdateTimeline.play();
    }

    private void animateWelcome() {
        if (welcomeLabel != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(800), welcomeLabel);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    @FXML
    private void handleStartPractice() {
        try {
            LOGGER.info("Starting practice session");
            App.setRoot("practice");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open practice", e);
            showError("Navigation Error", "Could not open practice mode");
        }
    }

    @FXML
    private void handleOpenLessons() {
        try {
            LOGGER.info("Opening lessons");
            App.setRoot("lessons");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open lessons", e);
            showError("Navigation Error", "Could not open lessons");
        }
    }

    @FXML
    private void handleOpenTuner() {
        try {
            LOGGER.info("Opening tuner");
            App.setRoot("tuner");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open tuner", e);
            showError("Navigation Error", "Could not open tuner");
        }
    }

    @FXML
    private void handleOpenTabs() {
        try {
            LOGGER.info("Opening tab player");
            App.setRoot("primary");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open tabs", e);
            showError("Navigation Error", "Could not open tab player");
        }
    }

    @FXML
    private void handleOpenStats() {
        try {
            LOGGER.info("Opening statistics");
            showAdvancedStats();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show stats", e);
            showError("Statistics Error", "Could not display statistics");
        }
    }

    private void showAdvancedStats() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Practice Statistics");
        alert.setHeaderText("📊 Your Progress Overview");
        
        alert.setContentText(String.format(
            "═══ PRACTICE SUMMARY ═══\n" +
            "🔥 Current Streak: %d days\n" +
            "🏆 Longest Streak: %d days\n" +
            "⏱️ Total Practice Time: %.1f hours\n" +
            "🎯 Average Accuracy: %d%%\n" +
            "⭐ Total Sessions: %d\n" +
            "🎵 Total Notes Played: %,d\n\n" +
            "═══ PROGRESS ═══\n" +
            "📚 Lessons Completed: %d / %d\n" +
            "📈 Overall Progress: %d%%",
            currentStreak,
            longestStreak,
            totalPracticeHours,
            averageAccuracy,
            totalSessions,
            totalNotesPlayed.get(),
            lessonsCompleted,
            totalLessons,
            (int)(overallProgress * 100)
        ));
        
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void cleanup() {
        LOGGER.info("Cleaning up MainController");
        
        if (clockTimeline != null) {
            clockTimeline.stop();
            clockTimeline = null;
        }
        
        if (statsUpdateTimeline != null) {
            statsUpdateTimeline.stop();
            statsUpdateTimeline = null;
        }
        
        sessionHistory.clear();
        achievements.clear();
        skillLevels.clear();
        practiceByDay.clear();
        sessionsByDate.clear();
    }

    private static class SessionData {
        final LocalDateTime dateTime;
        final String name;
        final int durationMinutes;
        final int rating;
        final int accuracy;
        final int notesPlayed;
        
        SessionData(LocalDateTime dateTime, String name, int durationMinutes, 
                   int rating, int accuracy, int notesPlayed) {
            this.dateTime = dateTime;
            this.name = name;
            this.durationMinutes = durationMinutes;
            this.rating = rating;
            this.accuracy = accuracy;
            this.notesPlayed = notesPlayed;
        }
    }

    private static class Achievement {
        final String name;
        final String description;
        boolean unlocked;
        
        Achievement(String name, String description, boolean unlocked) {
            this.name = name;
            this.description = description;
            this.unlocked = unlocked;
        }
    }

    public void addSession(String name, int durationMinutes, int rating, int accuracy, int notesPlayed) {
        SessionData newSession = new SessionData(
            LocalDateTime.now(), name, durationMinutes, rating, accuracy, notesPlayed
        );
        
        sessionHistory.add(0, newSession);
        sessionsByDate.put(newSession.dateTime.toLocalDate(), newSession);
        
        DayOfWeek day = newSession.dateTime.getDayOfWeek();
        practiceByDay.merge(day, 1, Integer::sum);
        
        if (accuracy >= 95) {
            perfectSessions++;
        }
        
        if (sessionHistory.size() > MAX_RECENT_SESSIONS) {
            sessionHistory.remove(sessionHistory.size() - 1);
        }
        
        totalSessions++;
        totalNotesPlayed.addAndGet(notesPlayed);
        loadRecentSessions();
        checkForMilestones();
    }

    public void updateProgress(int completedLessons) {
        this.lessonsCompleted = completedLessons;
        this.overallProgress = (double) completedLessons / totalLessons;
        
        Platform.runLater(() -> {
            if (overallProgressBar != null) {
                Timeline progressAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, 
                        new KeyValue(overallProgressBar.progressProperty(), 
                            overallProgressBar.getProgress())),
                    new KeyFrame(Duration.millis(1000), 
                        new KeyValue(overallProgressBar.progressProperty(), 
                            overallProgress, Interpolator.EASE_BOTH))
                );
                progressAnimation.play();
            }
            
            if (progressLabel != null) {
                int percentComplete = (int) (overallProgress * 100);
                progressLabel.setText(String.format("%d%% Complete - %d/%d Lessons", 
                    percentComplete, completedLessons, totalLessons));
            }
        });
    }
}