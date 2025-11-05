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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PrimaryController - Enhanced Tab Player with Real-time Note Detection
 * 
 * Features:
 * - Interactive tablature display with canvas rendering
 * - Real-time note detection and comparison
 * - Variable playback speed control (0.25x - 2.0x)
 * - Accuracy tracking and grading system
 * - Guitar calibration integration
 * - Visual feedback and animations
 * 
 * @version 1.1
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
        loadSampleTabs();
        startStatusAnimation();
        animateWelcome();
        pauseBtn.setDisable(true);
        startBtn.setDisable(true); // Disabled until tab is loaded and calibrated
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
                    setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-padding: 12;");
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
        
        // Smoke on the Water
        tabDatabase.put("🔥 Smoke on the Water - Deep Purple (Beginner)", 
            new TabData("Smoke on the Water", "Deep Purple", 1, 50, 90));
        tabNoteData.put("🔥 Smoke on the Water - Deep Purple (Beginner)", 
            generateTabNotes(50, 90)); // 50 notes, 90 BPM
        
        // Enter Sandman
        tabDatabase.put("⚡ Enter Sandman - Metallica (Intermediate)", 
            new TabData("Enter Sandman", "Metallica", 2, 120, 140));
        tabNoteData.put("⚡ Enter Sandman - Metallica (Intermediate)", 
            generateTabNotes(120, 140));
        
        // Stairway to Heaven
        tabDatabase.put("🌟 Stairway to Heaven - Led Zeppelin (Advanced)", 
            new TabData("Stairway to Heaven", "Led Zeppelin", 3, 200, 80));
        tabNoteData.put("🌟 Stairway to Heaven - Led Zeppelin (Advanced)", 
            generateTabNotes(200, 80));
        
        // Sweet Child O' Mine
        tabDatabase.put("🎵 Sweet Child O' Mine - Guns N' Roses (Intermediate)", 
            new TabData("Sweet Child O' Mine", "Guns N' Roses", 2, 150, 125));
        tabNoteData.put("🎵 Sweet Child O' Mine - Guns N' Roses (Intermediate)", 
            generateTabNotes(150, 125));
        
        // Back in Black
        tabDatabase.put("🔊 Back in Black - AC/DC (Beginner)", 
            new TabData("Back in Black", "AC/DC", 1, 80, 95));
        tabNoteData.put("🔊 Back in Black - AC/DC (Beginner)", 
            generateTabNotes(80, 95));
        
        tabListView.getItems().addAll(tabDatabase.keySet());
        LOGGER.info("Loaded " + tabDatabase.size() + " sample tabs");
    }

    /**
     * Generates realistic tab note data
     */
    private List<TabNote> generateTabNotes(int count, int bpm) {
        List<TabNote> notes = new ArrayList<>();
        int[] frets = {0, 3, 5, 7, 8, 10, 12};
        
        for (int i = 0; i < count; i++) {
            int string = random.nextInt(6); // 0-5
            int fret = frets[random.nextInt(frets.length)];
            long timestamp = (long) (i * (60000.0 / bpm)); // Convert BPM to ms between notes
            
            notes.add(new TabNote(string, fret, timestamp));
        }
        
        return notes;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            updateStatus("Please enter a search term! 🔍");
            shakeNode(searchField);
            return;
        }
        
        updateStatus("Searching for: " + query + "...");
        tabListView.getItems().clear();
        
        // Search in background thread to keep UI responsive
        new Thread(() -> {
            try {
                Thread.sleep(500); // Simulate API call
                Platform.runLater(() -> {
                    tabDatabase.keySet().stream()
                        .filter(tab -> tab.toLowerCase().contains(query.toLowerCase()))
                        .forEach(tab -> tabListView.getItems().add(tab));
                    
                    if (tabListView.getItems().isEmpty()) {
                        tabListView.getItems().add("No results found - try 'Smoke' or 'Metallica'");
                        updateStatus("No tabs found for: " + query);
                    } else {
                        updateStatus("Found " + tabListView.getItems().size() + " tabs! 🎸");
                        pulseNode(tabListView);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Search interrupted", e);
                Thread.currentThread().interrupt();
            }
        }, "Search-Thread").start();
    }

    /**
     * Shakes a node to indicate error
     */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    /**
     * Pulses a node to draw attention
     */
    private void pulseNode(javafx.scene.Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.03);
        pulse.setToY(1.03);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    @FXML
    private void handleCalibrate() {
        updateStatus("Calibrating guitar input... 🎚️");
        calibrateBtn.setDisable(true);
        
        RotateTransition rotate = new RotateTransition(Duration.millis(1000), calibrateBtn);
        rotate.setByAngle(360);
        rotate.setCycleCount(5);
        rotate.play();
        
        // Simulate calibration process
        new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    int string = i;
                    Platform.runLater(() -> {
                        updateStatus("Play string " + string + "/6... (" + STRING_NAMES[6 - string] + ")");
                    });
                    Thread.sleep(800);
                }
                
                Platform.runLater(() -> {
                    isCalibrated = true;
                    calibrationStatus.setText("✅ Calibrated");
                    calibrationStatus.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
                    updateStatus("Calibration complete! Ready to shred! 🤘");
                    calibrateBtn.setDisable(false);
                    pulseNode(calibrationStatus);
                    
                    // Enable start button if tab is loaded
                    if (currentTab != null) {
                        startBtn.setDisable(false);
                        pulseNode(startBtn);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Calibration interrupted", e);
                Thread.currentThread().interrupt();
            }
        }, "Calibration-Thread").start();
    }

    /**
     * Loads a tab and prepares for playback
     */
    private void loadTab(String tabKey) {
        if (!tabDatabase.containsKey(tabKey)) {
            LOGGER.warning("Tab not found: " + tabKey);
            return;
        }
        
        TabData tab = tabDatabase.get(tabKey);
        currentTab = tab.name;
        currentTabLabel.setText(tab.name + " - " + tab.artist);
        totalNotes = tab.noteCount;
        correctNotes = 0;
        attemptedNotes = 0;
        currentPosition = 0;
        streak = 0;
        
        updateStatus("Loaded: " + tab.name + " 🎵");
        drawTab(tab);
        updateStats();
        
        if (isCalibrated) {
            startBtn.setDisable(false);
            pulseNode(startBtn);
        } else {
            updateStatus("Please calibrate your guitar first! 🎚️");
            pulseNode(calibrateBtn);
        }
        
        LOGGER.info("Tab loaded: " + tab.name + " (" + tab.noteCount + " notes)");
    }

    /**
     * Draws the tab on the canvas with enhanced visuals
     */
    private void drawTab(TabData tab) {
        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();
        
        // Clear with gradient background
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, width, height);
        
        // Draw strings
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(2);
        
        for (int i = 1; i <= 6; i++) {
            double y = STRING_SPACING * i;
            gc.strokeLine(50, y, width - 50, y);
        }
        
        // String names on the left
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        for (int i = 0; i < 6; i++) {
            gc.fillText(STRING_NAMES[i], 20, STRING_SPACING * (i + 1) + 5);
        }
        
        // Draw tab notes
        List<TabNote> notes = tabNoteData.get(tabListView.getSelectionModel().getSelectedItem());
        if (notes != null) {
            drawTabNotes(gc, notes, width);
        }
        
        // Draw difficulty badge
        drawDifficultyBadge(gc, tab, width);
        
        // Draw tempo indicator
        drawTempoIndicator(gc, tab);
    }

    /**
     * Draws tab notes on the canvas
     */
    private void drawTabNotes(GraphicsContext gc, List<TabNote> notes, double width) {
        double noteX = 100;
        int displayCount = Math.min(VISIBLE_NOTES, notes.size());
        int startIndex = Math.max(0, currentPosition - 2);
        
        for (int i = 0; i < displayCount && (startIndex + i) < notes.size(); i++) {
            TabNote note = notes.get(startIndex + i);
            double y = STRING_SPACING * (note.string + 1);
            
            // Highlight current note
            if ((startIndex + i) == currentPosition && isPlaying) {
                gc.setFill(Color.web("#00ff88", 0.3));
                gc.fillOval(noteX - 20, y - 20, 40, 40);
                gc.setFill(Color.web("#00ff88"));
            } else {
                gc.setFill(NOTE_COLORS[i % NOTE_COLORS.length]);
            }
            
            // Draw fret number
            gc.setFont(Font.font("System", FontWeight.BOLD, 18));
            String fretText = String.valueOf(note.fret);
            gc.fillText(fretText, noteX - (fretText.length() * 4), y + 6);
            noteX += NOTE_SPACING;
        }
    }

    /**
     * Draws difficulty badge
     */
    private void drawDifficultyBadge(GraphicsContext gc, TabData tab, double width) {
        gc.setFill(Color.web("#ffaa00", 0.9));
        gc.fillRoundRect(width - 180, 20, 150, 40, 10, 10);
        gc.setFill(Color.web("#1a1a2e"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        String difficulty = tab.difficulty == 1 ? "🟢 Beginner" : 
                          tab.difficulty == 2 ? "🟡 Intermediate" : "🔴 Advanced";
        gc.fillText(difficulty, width - 170, 45);
    }

    /**
     * Draws tempo indicator
     */
    private void drawTempoIndicator(GraphicsContext gc, TabData tab) {
        gc.setFill(Color.web("#00d9ff", 0.2));
        gc.fillRoundRect(20, 20, 100, 40, 10, 10);
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        gc.fillText("♩ = " + tab.bpm + " BPM", 30, 45);
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
        double accuracy = totalNotes > 0 ? (double) correctNotes / attemptedNotes * 100 : 0;
        String grade = calculateGrade(accuracy);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Complete! 🎸");
        alert.setHeaderText("Great job! Here's your performance:");
        alert.setContentText(
            String.format("⏱️ Duration: %d:%02d\n", sessionDuration / 60, sessionDuration % 60) +
            String.format("🎯 Accuracy: %.1f%%\n", accuracy) +
            "📊 Grade: " + grade + "\n" +
            "🎵 Notes Hit: " + correctNotes + " / " + attemptedNotes + "\n" +
            "🔥 Max Streak: " + maxStreak + "\n\n" +
            getEncouragementMessage(accuracy)
        );
        alert.showAndWait();
        
        // Reset for next session
        currentPosition = 0;
        correctNotes = 0;
        attemptedNotes = 0;
        streak = 0;
        updateStats();
        progressBar.setProgress(0);
        
        LOGGER.info("Session completed with " + accuracy + "% accuracy");
    }

    /**
     * Calculates letter grade from accuracy percentage
     */
    private String calculateGrade(double accuracy) {
        if (accuracy >= 95) return "A+";
        if (accuracy >= 90) return "A";
        if (accuracy >= 85) return "B+";
        if (accuracy >= 80) return "B";
        if (accuracy >= 75) return "C+";
        if (accuracy >= 70) return "C";
        return "D";
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
        gradeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        
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