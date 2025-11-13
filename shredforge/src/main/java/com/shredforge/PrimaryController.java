package com.shredforge;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PrimaryController - Enhanced Tab Player with Real-time Note Detection
 * 
 * Features:
 * - Interactive tablature display with canvas rendering
 * - Real-time note detection simulation
 * - Variable playback speed control (0.25x - 2.0x)
 * - Enhanced accuracy tracking and grading system
 * - Visual feedback and animations
 * - Search functionality for tabs
 * - Expanded tab library
 * 
 * @version 1.3
 * @author Team 2 - ShredForge
 */
public class PrimaryController {
    
    private static final Logger LOGGER = Logger.getLogger(PrimaryController.class.getName());

    // FXML Components
    @FXML private TextField searchField;
    @FXML private ListView<String> tabListView;
    @FXML private Canvas tabCanvas;
    @FXML private Button calibrateBtn;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Slider speedSlider;
    @FXML private Label statusLabel;
    @FXML private Label speedLabel;
    @FXML private Label accuracyLabel;
    @FXML private Label notesLabel;
    @FXML private Label gradeLabel;
    @FXML private Label calibrationStatus;
    @FXML private Label currentTabLabel;
    @FXML private ProgressBar progressBar;
    
    // State Management
    private boolean isCalibrated = false;
    private boolean isPlaying = false;
    private String currentTab = null;
    private int totalNotes = 0;
    private int correctNotes = 0;
    private int attemptedNotes = 0;
    private int currentPosition = 0;
    private double currentSpeed = 1.0;
    
    // Playback Control
    private Timeline playbackTimeline;
    private Timeline statusBlinkTimeline;
    private Random random = new Random();
    
    // Tab Database
    private Map<String, TabData> tabDatabase = new ConcurrentHashMap<>();
    private Map<String, List<TabNote>> tabNoteData = new ConcurrentHashMap<>();
    
    // Performance Tracking
    private long sessionStartTime = 0;
    private int streak = 0;
    private int maxStreak = 0;
    private double averageAccuracy = 0.0;
    
    // Canvas Drawing
    private static final double STRING_SPACING = 50.0;
    private static final double NOTE_SPACING = 40.0;
    private static final int VISIBLE_NOTES = 15;
    private static final String[] STRING_NAMES = {"E", "B", "G", "D", "A", "E"};
    private static final Color[] NOTE_COLORS = {
        Color.web("#e94560"), Color.web("#00ff88"), 
        Color.web("#00d9ff"), Color.web("#ffaa00"),
        Color.web("#a855f7"), Color.web("#ff6b6b")
    };

    @FXML
    public void initialize() {
        LOGGER.info("Initializing PrimaryController");
        setupCanvas();
        setupSlider();
        setupListView();
        setupSearchField();
        loadSampleTabs();
        startStatusAnimation();
        animateWelcome();
        pauseBtn.setDisable(true);
        startBtn.setDisable(true); // Disabled until tab is loaded and calibrated
        LOGGER.info("PrimaryController initialized successfully");
    }

    /**
     * Animates the welcome screen with fade effect
     */
    private void animateWelcome() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), tabCanvas);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Sets up the canvas with initial display
     */
    private void setupCanvas() {
        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, tabCanvas.getWidth(), tabCanvas.getHeight());
        
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 24));
        gc.fillText("🎸 Select a tab to begin your journey!", 150, 180);
        
        // Draw subtle grid
        gc.setStroke(Color.web("#1a1a2e"));
        gc.setLineWidth(1);
        for (int i = 0; i < 700; i += 50) {
            gc.strokeLine(i, 0, i, 350);
        }
        for (int i = 0; i < 350; i += 50) {
            gc.strokeLine(0, i, 700, i);
        }
    }

    /**
     * Sets up the speed slider with listener
     */
    private void setupSlider() {
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentSpeed = newVal.doubleValue();
            speedLabel.setText(String.format("%.2fx", currentSpeed));
            animateLabel(speedLabel);
            
            // Update playback speed if playing
            if (isPlaying && playbackTimeline != null) {
                playbackTimeline.setRate(currentSpeed);
            }
        });
        
        // Initialize speed label
        speedLabel.setText(String.format("%.2fx", speedSlider.getValue()));
    }

    /**
     * Sets up the tab list view with custom cell factory
     */
    private void setupListView() {
        tabListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    final String baseStyle = "-fx-text-fill: white; " +
                           "-fx-background-color: transparent; " +
                           "-fx-padding: 12; " +
                           "-fx-font-size: 14px;";
                    
                    final String hoverStyle = baseStyle +
                           "-fx-background-color: rgba(0, 217, 255, 0.15); " +
                           "-fx-cursor: hand;";
                    
                    setStyle(baseStyle);
                    setOnMouseEntered(e -> setStyle(hoverStyle));
                    setOnMouseExited(e -> setStyle(baseStyle));
                }
            }
        });
        
        tabListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTab(newVal);
                animateTabLoad();
            }
        });
    }

    /**
     * Sets up search field functionality
     */
    private void setupSearchField() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterTabs(newValue);
            });
        }
    }

    /**
     * Filters tabs based on search query
     */
    private void filterTabs(String query) {
        if (query == null || query.trim().isEmpty()) {
            tabListView.setItems(javafx.collections.FXCollections.observableArrayList(tabDatabase.keySet()));
            return;
        }
        
        List<String> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (String tabName : tabDatabase.keySet()) {
            if (tabName.toLowerCase().contains(lowerQuery)) {
                filtered.add(tabName);
            }
        }
        
        tabListView.setItems(javafx.collections.FXCollections.observableArrayList(filtered));
        LOGGER.info("Filtered tabs: " + filtered.size() + " results");
    }

    /**
     * Animates tab loading with scale effect
     */
    private void animateTabLoad() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), tabCanvas);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    /**
     * Animates a label with scale effect
     */
    private void animateLabel(Label label) {
        if (label == null) return;
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), label);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.15);
        scale.setToY(1.15);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    /**
     * Loads sample tabs into the database with realistic note data
     */
    private void loadSampleTabs() {
        LOGGER.info("Loading sample tabs");
        
        // Beginner tabs
        addTab("🔥 Smoke on the Water - Deep Purple (Beginner)", 
            "Smoke on the Water", "Deep Purple", 1, 50, 90);
        
        addTab("⭐ Seven Nation Army - The White Stripes (Beginner)", 
            "Seven Nation Army", "The White Stripes", 1, 40, 110);
        
        // Intermediate tabs
        addTab("⚡ Enter Sandman - Metallica (Intermediate)", 
            "Enter Sandman", "Metallica", 2, 120, 140);
        
        addTab("🎵 Sweet Child O' Mine - Guns N' Roses (Intermediate)", 
            "Sweet Child O' Mine", "Guns N' Roses", 2, 150, 125);
        
        addTab("🎸 Nothing Else Matters - Metallica (Intermediate)", 
            "Nothing Else Matters", "Metallica", 2, 100, 100);
        
        // Advanced tabs
        addTab("🌟 Stairway to Heaven - Led Zeppelin (Advanced)", 
            "Stairway to Heaven", "Led Zeppelin", 3, 200, 80);
        
        addTab("🔥 Master of Puppets - Metallica (Advanced)", 
            "Master of Puppets", "Metallica", 3, 220, 150);
        
        addTab("⚡ Eruption - Van Halen (Advanced)", 
            "Eruption", "Van Halen", 3, 180, 200);
        
        // Update list view
        tabListView.setItems(javafx.collections.FXCollections.observableArrayList(tabDatabase.keySet()));
        
        LOGGER.info("Loaded " + tabDatabase.size() + " tabs");
    }

    /**
     * Helper method to add a tab
     */
    private void addTab(String displayName, String name, String artist, int difficulty, int noteCount, int bpm) {
        tabDatabase.put(displayName, new TabData(name, artist, difficulty, noteCount, bpm));
        tabNoteData.put(displayName, generateTabNotes(noteCount, bpm));
    }

    /**
     * Generates realistic tab note data
     */
    private List<TabNote> generateTabNotes(int count, int bpm) {
        List<TabNote> notes = new ArrayList<>();
        long timePerBeat = (long) (60000.0 / bpm); // ms per beat
        
        for (int i = 0; i < count; i++) {
            int string = random.nextInt(6);
            int fret = random.nextInt(12);
            long timestamp = i * timePerBeat;
            notes.add(new TabNote(string, fret, timestamp));
        }
        
        return notes;
    }

    /**
     * Loads a tab and prepares for playback
     */
    private void loadTab(String tabKey) {
        TabData tab = tabDatabase.get(tabKey);
        if (tab == null) {
            LOGGER.warning("Tab not found: " + tabKey);
            return;
        }
        
        currentTab = tab.name;
        totalNotes = tab.noteCount;
        currentPosition = 0;
        correctNotes = 0;
        attemptedNotes = 0;
        streak = 0;
        maxStreak = 0;
        
        currentTabLabel.setText(tab.name + " - " + tab.artist);
        updateStats();
        drawTab(tab);
        
        // Enable start button if calibrated
        if (isCalibrated) {
            startBtn.setDisable(false);
        }
        
        updateStatus("Tab loaded: " + tab.name + " 🎸");
        LOGGER.info("Loaded tab: " + tab.name);
    }

    /**
     * Draws the tab on canvas
     */
    private void drawTab(TabData tab) {
        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();
        
        // Clear and draw background
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, width, height);
        
        // Draw strings
        gc.setStroke(Color.web("#444"));
        gc.setLineWidth(2);
        for (int i = 0; i < 6; i++) {
            double y = (height / 7) * (i + 1);
            gc.strokeLine(50, y, width - 50, y);
            
            // String names
            gc.setFill(NOTE_COLORS[i]);
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            gc.fillText(STRING_NAMES[i], 20, y + 5);
        }
        
        // Draw title
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.fillText(tab.name + " - " + tab.artist, width / 2 - 100, 30);
        
        // Draw BPM and difficulty
        gc.setFill(Color.web("#ffaa00"));
        gc.setFont(Font.font("System", 14));
        String difficultyText = getDifficultyText(tab.difficulty);
        gc.fillText("♩ = " + tab.bpm + " BPM | " + difficultyText, width - 250, 30);
        
        // Draw some sample notes
        drawSampleNotes(gc, tab);
        
        // Draw playback position indicator if playing
        if (isPlaying && totalNotes > 0) {
            double progress = (double) currentPosition / totalNotes;
            double indicatorX = 50 + progress * (width - 100);
            gc.setStroke(Color.web("#e94560"));
            gc.setLineWidth(3);
            gc.strokeLine(indicatorX, 50, indicatorX, height - 50);
        }
    }

    /**
     * Draws sample notes on the tab
     */
    private void drawSampleNotes(GraphicsContext gc, TabData tab) {
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();
        
        List<TabNote> notes = tabNoteData.get(tabListView.getSelectionModel().getSelectedItem());
        if (notes == null) return;
        
        int startIndex = Math.max(0, currentPosition - 5);
        int endIndex = Math.min(notes.size(), currentPosition + VISIBLE_NOTES);
        
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        for (int i = startIndex; i < endIndex; i++) {
            TabNote note = notes.get(i);
            double x = 100 + ((i - startIndex) * NOTE_SPACING);
            double y = (height / 7) * (note.string + 1);
            
            // Highlight current note
            if (i == currentPosition) {
                gc.setFill(Color.web("#e94560"));
                gc.fillOval(x - 15, y - 15, 30, 30);
            }
            
            // Draw fret number
            gc.setFill(i == currentPosition ? Color.WHITE : NOTE_COLORS[note.string]);
            gc.fillText(String.valueOf(note.fret), x - 5, y + 6);
        }
    }

    /**
     * Gets difficulty text from number
     */
    private String getDifficultyText(int difficulty) {
        switch (difficulty) {
            case 1: return "⭐ Beginner";
            case 2: return "⭐⭐ Intermediate";
            case 3: return "⭐⭐⭐ Advanced";
            default: return "Unknown";
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        LOGGER.info("Searching for: " + query);
        filterTabs(query);
    }

    @FXML
    private void handleCalibrate() {
        isCalibrated = true;
        calibrationStatus.setText("✓ Calibrated");
        calibrationStatus.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        if (currentTab != null) {
            startBtn.setDisable(false);
        }
        
        updateStatus("Calibration complete! ✓");
        pulseNode(calibrateBtn);
        
        LOGGER.info("Guitar calibrated");
    }

    /**
     * Pulses a node for visual feedback
     */
    private void pulseNode(Node node) {
        if (node == null) return;
        
        ScaleTransition pulse = new ScaleTransition(Duration.millis(150), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    /**
     * Shakes a node to indicate error
     */
    private void shakeNode(Node node) {
        if (node == null) return;
        
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    @FXML
    private void handleStart() {
        if (currentTab == null) {
            updateStatus("Please select a tab first! 📋");
            shakeNode(tabListView);
            return;
        }
        
        if (!isCalibrated) {
            updateStatus("Please calibrate your guitar first! 🎚️");
            shakeNode(calibrateBtn);
            return;
        }
        
        isPlaying = true;
        sessionStartTime = System.currentTimeMillis();
        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
        updateStatus("Session started! Play along! 🎸");
        pulseNode(tabCanvas);
        
        TabData tab = tabDatabase.get(tabListView.getSelectionModel().getSelectedItem());
        List<TabNote> notes = tabNoteData.get(tabListView.getSelectionModel().getSelectedItem());
        
        if (notes == null || notes.isEmpty()) {
            LOGGER.warning("No notes found for tab");
            return;
        }
        
        // Calculate interval based on BPM and speed
        double baseInterval = (60000.0 / tab.bpm) / 2; // Half note duration in ms
        double adjustedInterval = baseInterval / currentSpeed;
        
        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(adjustedInterval), event -> {
            currentPosition++;
            double progress = (double) currentPosition / totalNotes;
            progressBar.setProgress(progress);
            
            // Simulate note detection and scoring
            if (currentPosition % 3 == 0) { // Simulate hits
                double hitAccuracy = 0.7 + (random.nextDouble() * 0.3); // 70-100%
                if (hitAccuracy > 0.85) {
                    correctNotes++;
                    streak++;
                    maxStreak = Math.max(maxStreak, streak);
                } else {
                    streak = 0;
                }
                attemptedNotes++;
                updateStats();
            }
            
            // Redraw tab with current position
            if (currentTab != null) {
                String tabKey = tabListView.getSelectionModel().getSelectedItem();
                if (tabKey != null && tabDatabase.containsKey(tabKey)) {
                    drawTab(tabDatabase.get(tabKey));
                }
            }
            
            // Check for completion
            if (currentPosition >= totalNotes) {
                handlePause();
                showFinalScore();
            }
        }));
        
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        playbackTimeline.setRate(currentSpeed);
        playbackTimeline.play();
        
        LOGGER.info("Playback started at " + currentSpeed + "x speed");
    }

    @FXML
    private void handlePause() {
        isPlaying = false;
        startBtn.setDisable(false);
        startBtn.setText("▶️ Resume");
        pauseBtn.setDisable(true);
        
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        
        updateStatus("Session paused ⏸️");
        LOGGER.info("Playback paused");
    }

    /**
     * Shows final score with detailed statistics
     */
    private void showFinalScore() {
        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000;
        double accuracy = attemptedNotes > 0 ? (double) correctNotes / attemptedNotes * 100 : 0;
        String grade = calculateGrade(accuracy);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Complete! 🎸");
        alert.setHeaderText("Great job! Here's your performance:");
        alert.setContentText(
            String.format("⏱️ Duration: %d:%02d\n", sessionDuration / 60, sessionDuration % 60) +
            String.format("🎯 Accuracy: %.1f%%\n", accuracy) +
            "📊 Grade: " + grade + "\n" +
            "🎵 Notes Hit: " + correctNotes + " / " + attemptedNotes + "\n" +
            "🔥 Max Streak: " + maxStreak + "\n" +
            "⚡ Speed: " + String.format("%.2fx", currentSpeed) + "\n\n" +
            getEncouragementMessage(accuracy)
        );
        alert.showAndWait();
        
        // Reset for next session
        currentPosition = 0;
        correctNotes = 0;
        attemptedNotes = 0;
        streak = 0;
        startBtn.setText("▶️ Start");
        updateStats();
        progressBar.setProgress(0);
        
        LOGGER.info("Session completed with " + accuracy + "% accuracy");
    }

    /**
     * Calculates letter grade from accuracy percentage
     */
    private String calculateGrade(double accuracy) {
        if (accuracy >= 95) return "A+ ⭐⭐⭐⭐⭐";
        if (accuracy >= 90) return "A ⭐⭐⭐⭐";
        if (accuracy >= 85) return "B+ ⭐⭐⭐⭐";
        if (accuracy >= 80) return "B ⭐⭐⭐";
        if (accuracy >= 75) return "C+ ⭐⭐";
        if (accuracy >= 70) return "C ⭐⭐";
        return "D ⭐";
    }

    /**
     * Returns encouragement message based on performance
     */
    private String getEncouragementMessage(double accuracy) {
        if (accuracy >= 95) return "Outstanding! You're a true shredder! 🤘🔥";
        if (accuracy >= 85) return "Excellent work! Keep it up! 🎸⭐";
        if (accuracy >= 75) return "Good job! Practice makes perfect! 🎵";
        if (accuracy >= 65) return "Nice effort! Try slowing down the tempo. 👍";
        return "Keep practicing! You'll get there! 💪🎸";
    }

    /**
     * Updates statistics display
     */
    private void updateStats() {
        double accuracy = attemptedNotes > 0 ? (double) correctNotes / attemptedNotes * 100 : 0;
        accuracyLabel.setText(String.format("%.1f%%", accuracy));
        notesLabel.setText(correctNotes + " / " + attemptedNotes);
        
        String grade = calculateGrade(accuracy);
        gradeLabel.setText(grade);
        
        String color = accuracy >= 90 ? "#00ff88" :
                      accuracy >= 80 ? "#00d9ff" :
                      accuracy >= 70 ? "#ffaa00" : "#ff6b6b";
        gradeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        // Add streak indicator
        if (streak >= 5) {
            gradeLabel.setText(grade + " 🔥");
        }
    }

    /**
     * Updates status message with animation
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
        animateLabel(statusLabel);
    }

    /**
     * Starts status label blinking animation
     */
    private void startStatusAnimation() {
        statusBlinkTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> statusLabel.setOpacity(1.0)),
            new KeyFrame(Duration.seconds(0.8), e -> statusLabel.setOpacity(0.7)),
            new KeyFrame(Duration.seconds(1.6), e -> statusLabel.setOpacity(1.0))
        );
        statusBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
        statusBlinkTimeline.play();
    }

    /**
     * Cleanup method called when controller is destroyed
     */
    public void cleanup() {
        LOGGER.info("Cleaning up PrimaryController");
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }
        if (statusBlinkTimeline != null) {
            statusBlinkTimeline.stop();
        }
    }

    /**
     * Inner class representing tab metadata
     */
    private static class TabData {
        String name;
        String artist;
        int difficulty;
        int noteCount;
        int bpm;
        
        TabData(String name, String artist, int difficulty, int noteCount, int bpm) {
            this.name = name;
            this.artist = artist;
            this.difficulty = difficulty;
            this.noteCount = noteCount;
            this.bpm = bpm;
        }
    }

    /**
     * Inner class representing a single note in the tab
     */
    private static class TabNote {
        int string;  // 0-5 (high E to low E)
        int fret;    // 0-24
        long timestamp; // Time in ms from start
        
        TabNote(int string, int fret, long timestamp) {
            this.string = string;
            this.fret = fret;
            this.timestamp = timestamp;
        }
    }
}