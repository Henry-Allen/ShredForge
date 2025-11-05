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

public class PrimaryController {
    
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
    
    private boolean isCalibrated = false;
    private boolean isPlaying = false;
    private String currentTab = null;
    private int totalNotes = 100;
    private int correctNotes = 0;
    private int currentPosition = 0;
    private Timeline playbackTimeline;
    private Random random = new Random();
    
    private Map<String, TabData> tabDatabase = new HashMap<>();
    
    @FXML
    public void initialize() {
        setupCanvas();
        setupSlider();
        setupListView();
        loadSampleTabs();
        startStatusAnimation();
        
        // Add main menu button
        addMainMenuButton();
    }
    
    private void addMainMenuButton() {
        // Navigate to main dashboard
        Button mainBtn = new Button("🏠 Dashboard");
        mainBtn.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #1a1a2e; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;");
        mainBtn.setOnAction(e -> {
            try {
                App.setRoot("main");
            } catch (Exception ex) {
                showError("Navigation failed: " + ex.getMessage());
            }
        });
    }
    
    private void setupCanvas() {
        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, tabCanvas.getWidth(), tabCanvas.getHeight());
        
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 24));
        gc.fillText("Select a tab to begin! 🎸", 180, 150);
    }
    
    private void setupSlider() {
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            speedLabel.setText(String.format("%.2fx", newVal.doubleValue()));
        });
    }
    
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
                    setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-padding: 10;");
                }
            }
        });
        
        tabListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTab(newVal);
            }
        });
    }
    
    private void loadSampleTabs() {
        tabDatabase.put("🔥 Smoke on the Water - Deep Purple (Beginner)", 
            new TabData("Smoke on the Water", "Deep Purple", 1, 50));
        tabDatabase.put("⚡ Enter Sandman - Metallica (Intermediate)", 
            new TabData("Enter Sandman", "Metallica", 2, 120));
        tabDatabase.put("🌟 Stairway to Heaven - Led Zeppelin (Advanced)", 
            new TabData("Stairway to Heaven", "Led Zeppelin", 3, 200));
        tabDatabase.put("🎵 Sweet Child O' Mine - Guns N' Roses (Intermediate)", 
            new TabData("Sweet Child O' Mine", "Guns N' Roses", 2, 150));
        tabDatabase.put("🔊 Back in Black - AC/DC (Beginner)", 
            new TabData("Back in Black", "AC/DC", 1, 80));
        
        tabListView.getItems().addAll(tabDatabase.keySet());
    }
    
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            updateStatus("Please enter a search term! 🔍");
            return;
        }
        
        updateStatus("Searching for: " + query + "...");
        tabListView.getItems().clear();
        
        new Thread(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    tabDatabase.keySet().stream()
                        .filter(tab -> tab.toLowerCase().contains(query.toLowerCase()))
                        .forEach(tab -> tabListView.getItems().add(tab));
                    
                    if (tabListView.getItems().isEmpty()) {
                        tabListView.getItems().add("No results found - try 'Smoke' or 'Metallica'");
                        updateStatus("No tabs found for: " + query);
                    } else {
                        updateStatus("Found " + tabListView.getItems().size() + " tabs! 🎸");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    private void handleCalibrate() {
        updateStatus("Calibrating guitar input... 🎚️");
        calibrateBtn.setDisable(true);
        
        new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    int string = i;
                    Platform.runLater(() -> {
                        updateStatus("Play string " + string + "/6...");
                    });
                    Thread.sleep(800);
                }
                
                Platform.runLater(() -> {
                    isCalibrated = true;
                    calibrationStatus.setText("✅ Calibrated");
                    calibrationStatus.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
                    updateStatus("Calibration complete! Ready to shred! 🤘");
                    calibrateBtn.setDisable(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void loadTab(String tabKey) {
        if (!tabDatabase.containsKey(tabKey)) return;
        
        TabData tab = tabDatabase.get(tabKey);
        currentTab = tab.name;
        currentTabLabel.setText(tab.name + " - " + tab.artist);
        totalNotes = tab.noteCount;
        correctNotes = 0;
        currentPosition = 0;
        
        updateStatus("Loaded: " + tab.name + " 🎵");
        drawTab(tab);
        updateStats();
        
        if (isCalibrated) {
            startBtn.setDisable(false);
        }
    }
    
    private void drawTab(TabData tab) {
        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();
        
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, width, height);
        
        gc.setStroke(Color.web("#333"));
        gc.setLineWidth(1);
        double stringSpacing = height / 7;
        
        for (int i = 1; i <= 6; i++) {
            double y = stringSpacing * i;
            gc.strokeLine(50, y, width - 50, y);
        }
        
        gc.setFill(Color.web("#00d9ff"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        String[] notes = {"E", "B", "G", "D", "A", "E"};
        for (int i = 0; i < 6; i++) {
            gc.fillText(notes[i], 20, stringSpacing * (i + 1) + 5);
        }
        
        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        double noteX = 100;
        int[] frets = {0, 3, 5, 7, 8, 10, 12};
        
        for (int i = 0; i < 15; i++) {
            int string = random.nextInt(6) + 1;
            int fret = frets[random.nextInt(frets.length)];
            double y = stringSpacing * string;
            
            if (i == currentPosition / 5 && isPlaying) {
                gc.setFill(Color.web("#00ff88"));
                gc.fillOval(noteX - 15, y - 15, 30, 30);
                gc.setFill(Color.web("#1a1a2e"));
            } else {
                gc.setFill(Color.web("#e94560"));
            }
            
            gc.fillText(String.valueOf(fret), noteX - 5, y + 5);
            noteX += 35;
            
            gc.setFill(Color.web("#e94560"));
        }
        
        gc.setFill(Color.web("#ffaa00"));
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        String difficulty = tab.difficulty == 1 ? "Beginner" : 
                          tab.difficulty == 2 ? "Intermediate" : "Advanced";
        gc.fillText("Difficulty: " + difficulty, width - 150, 30);
    }
    
    @FXML
    private void handleStart() {
        if (currentTab == null) {
            updateStatus("Please select a tab first! 📋");
            return;
        }
        
        if (!isCalibrated) {
            updateStatus("Please calibrate your guitar first! 🎚️");
            return;
        }
        
        isPlaying = true;
        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
        updateStatus("Session started! Play along! 🎸");
        
        double speed = speedSlider.getValue();
        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(100 / speed), e -> {
            currentPosition++;
            double progress = (double) currentPosition / totalNotes;
            progressBar.setProgress(progress);
            
            if (currentPosition % 5 == 0 && random.nextDouble() > 0.3) {
                correctNotes++;
                updateStats();
            }
            
            if (currentTab != null && currentPosition % 5 == 0) {
                String tabKey = tabListView.getSelectionModel().getSelectedItem();
                if (tabKey != null && tabDatabase.containsKey(tabKey)) {
                    drawTab(tabDatabase.get(tabKey));
                }
            }
            
            if (currentPosition >= totalNotes) {
                handlePause();
                showFinalScore();
            }
        }));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        playbackTimeline.play();
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
    }
    
    private void showFinalScore() {
        double accuracy = (double) correctNotes / totalNotes * 100;
        String grade = accuracy >= 95 ? "A+" :
                      accuracy >= 90 ? "A" :
                      accuracy >= 85 ? "B+" :
                      accuracy >= 80 ? "B" :
                      accuracy >= 75 ? "C+" :
                      accuracy >= 70 ? "C" : "D";
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Complete! 🎸");
        alert.setHeaderText("Great job! Here's your performance:");
        alert.setContentText(
            "Accuracy: " + String.format("%.1f%%\n", accuracy) +
            "Grade: " + grade + "\n" +
            "Notes Hit: " + correctNotes + " / " + totalNotes + "\n\n" +
            "Keep practicing to improve! 🤘"
        );
        alert.showAndWait();
        
        currentPosition = 0;
        correctNotes = 0;
        updateStats();
        progressBar.setProgress(0);
    }
    
    private void updateStats() {
        double accuracy = totalNotes > 0 ? (double) correctNotes / Math.max(currentPosition, 1) * 100 : 0;
        accuracyLabel.setText(String.format("%.1f%%", accuracy));
        notesLabel.setText(correctNotes + " / " + totalNotes);
        
        String grade = accuracy >= 95 ? "A+" :
                      accuracy >= 90 ? "A" :
                      accuracy >= 85 ? "B+" :
                      accuracy >= 80 ? "B" :
                      accuracy >= 75 ? "C+" :
                      accuracy >= 70 ? "C" : "D";
        gradeLabel.setText(grade);
        
        String color = accuracy >= 90 ? "#00ff88" :
                      accuracy >= 80 ? "#00d9ff" :
                      accuracy >= 70 ? "#ffaa00" : "#ff6b6b";
        gradeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px; -fx-font-weight: bold;");
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private void startStatusAnimation() {
        Timeline statusBlink = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> statusLabel.setOpacity(1.0)),
            new KeyFrame(Duration.seconds(0.5), e -> statusLabel.setOpacity(0.7)),
            new KeyFrame(Duration.seconds(1.0), e -> statusLabel.setOpacity(1.0))
        );
        statusBlink.setCycleCount(Timeline.INDEFINITE);
        statusBlink.play();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private static class TabData {
        String name;
        String artist;
        int difficulty;
        int noteCount;
        
        TabData(String name, String artist, int difficulty, int noteCount) {
            this.name = name;
            this.artist = artist;
            this.difficulty = difficulty;
            this.noteCount = noteCount;
        }
    }
}
