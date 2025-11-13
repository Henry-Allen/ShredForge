package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.Tab;
import com.shredforge.repository.ShredForgeRepository;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for tab player with visual rendering
 * Simulates tab playback and note detection
 */
public class PrimaryController {

    private static final Logger LOGGER = Logger.getLogger(PrimaryController.class.getName());

    @FXML
    private Canvas tabCanvas;

    @FXML
    private ListView<String> tabListView;

    @FXML
    private Button playButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button stopButton;

    @FXML
    private Slider speedSlider;

    @FXML
    private Label speedLabel;

    @FXML
    private Label currentTabLabel;

    @FXML
    private Label statusLabel;

    private ShredForgeRepository repository;
    private Tab currentTab;
    private boolean isPlaying = false;
    private int currentPosition = 0;
    private AnimationTimer playbackTimer;
    private List<SimulatedNote> simulatedNotes;

    @FXML
    public void initialize() {
        repository = ShredForgeRepository.getInstance();
        setupTabList();
        setupCanvas();
        setupControls();

        // Apply fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        if (tabCanvas != null) {
            fadeIn.setNode(tabCanvas.getParent());
            fadeIn.play();
        }

        LOGGER.info("PrimaryController initialized");
    }

    /**
     * Setup tab list view
     */
    private void setupTabList() {
        if (tabListView == null) return;

        try {
            List<Tab> tabs = repository.getAllTabs();
            for (Tab tab : tabs) {
                String displayText = tab.getTitle() + " - " + tab.getArtist();
                tabListView.getItems().add(displayText);
            }

            // Handle tab selection
            tabListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadSelectedTab(newVal);
                }
            });

            LOGGER.info("Loaded " + tabs.size() + " tabs");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load tabs", e);
        }
    }

    /**
     * Setup canvas for tab rendering
     */
    private void setupCanvas() {
        if (tabCanvas != null) {
            drawEmptyTab();
        }
    }

    /**
     * Setup playback controls
     */
    private void setupControls() {
        if (speedSlider != null) {
            speedSlider.setValue(100);
            speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateSpeedLabel(newVal.intValue());
            });
            updateSpeedLabel(100);
        }

        if (pauseButton != null) pauseButton.setDisable(true);
        if (stopButton != null) stopButton.setDisable(true);
    }

    /**
     * Load the selected tab
     */
    private void loadSelectedTab(String selectedText) {
        try {
            String title = selectedText.split(" - ")[0];
            List<Tab> tabs = repository.getAllTabs();

            for (Tab tab : tabs) {
                if (tab.getTitle().equals(title)) {
                    currentTab = tab;
                    if (currentTabLabel != null) {
                        currentTabLabel.setText(tab.getTitle() + " - " + tab.getArtist());
                    }

                    // Generate simulated notes for visualization
                    generateSimulatedNotes();

                    drawTab();
                    LOGGER.info("Loaded tab: " + tab.getTitle());
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load tab", e);
        }
    }

    /**
     * Generate simulated notes for playback visualization
     */
    private void generateSimulatedNotes() {
        simulatedNotes = new ArrayList<>();

        // Create a simple melody pattern for demonstration
        int[] strings = {1, 2, 3, 2, 1, 2, 3, 4, 3, 2, 1, 2};
        int[] frets = {0, 2, 3, 2, 0, 2, 3, 5, 3, 2, 0, 2};

        for (int i = 0; i < strings.length; i++) {
            simulatedNotes.add(new SimulatedNote(strings[i], frets[i], i * 500));
        }
    }

    /**
     * Update speed label
     */
    private void updateSpeedLabel(int speed) {
        if (speedLabel != null) {
            speedLabel.setText(speed + "%");
        }
    }

    /**
     * Draw empty tab on canvas
     */
    private void drawEmptyTab() {
        if (tabCanvas == null) return;

        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();

        // Clear canvas
        gc.setFill(Color.rgb(10, 10, 10));
        gc.fillRect(0, 0, width, height);

        // Draw strings
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1);

        double stringSpacing = height / 7;
        for (int i = 0; i < 6; i++) {
            double y = stringSpacing * (i + 1);
            gc.strokeLine(20, y, width - 20, y);
        }

        // Draw labels
        gc.setFill(Color.rgb(136, 136, 136));
        String[] tuning = {"E", "A", "D", "G", "B", "e"};
        for (int i = 0; i < 6; i++) {
            double y = stringSpacing * (i + 1);
            gc.fillText(tuning[i], 5, y + 5);
        }
    }

    /**
     * Draw tab with notes
     */
    private void drawTab() {
        if (tabCanvas == null || simulatedNotes == null) {
            drawEmptyTab();
            return;
        }

        GraphicsContext gc = tabCanvas.getGraphicsContext2D();
        double width = tabCanvas.getWidth();
        double height = tabCanvas.getHeight();

        // Draw empty tab first
        drawEmptyTab();

        // Draw simulated notes
        double stringSpacing = height / 7;
        double noteSpacing = (width - 100) / simulatedNotes.size();

        for (int i = 0; i < simulatedNotes.size(); i++) {
            SimulatedNote note = simulatedNotes.get(i);
            double x = 50 + i * noteSpacing;
            double y = stringSpacing * note.getString();

            // Highlight current note
            if (i == currentPosition && isPlaying) {
                gc.setFill(Color.rgb(233, 69, 96, 0.3));
                gc.fillOval(x - 15, y - 15, 30, 30);
            }

            // Draw fret number
            gc.setFill(i == currentPosition && isPlaying ? Color.rgb(0, 255, 136) : Color.rgb(0, 217, 255));
            gc.fillText(String.valueOf(note.getFret()), x - 3, y + 5);

            // Draw note circle
            gc.setStroke(i == currentPosition && isPlaying ? Color.rgb(0, 255, 136) : Color.rgb(0, 217, 255));
            gc.setLineWidth(2);
            gc.strokeOval(x - 10, y - 10, 20, 20);
        }
    }

    /**
     * Start playback
     */
    @FXML
    private void handlePlay() {
        if (currentTab == null) {
            DialogHelper.showWarning("No Tab Selected", "Please select a tab from the list first");
            return;
        }

        if (isPlaying) return;

        isPlaying = true;
        currentPosition = 0;

        if (playButton != null) playButton.setDisable(true);
        if (pauseButton != null) pauseButton.setDisable(false);
        if (stopButton != null) stopButton.setDisable(false);
        if (statusLabel != null) statusLabel.setText("Playing...");

        startPlaybackTimer();

        LOGGER.info("Playback started");
    }

    /**
     * Pause playback
     */
    @FXML
    private void handlePause() {
        isPlaying = false;

        if (playButton != null) playButton.setDisable(false);
        if (pauseButton != null) pauseButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Paused");

        if (playbackTimer != null) {
            playbackTimer.stop();
        }

        LOGGER.info("Playback paused");
    }

    /**
     * Stop playback
     */
    @FXML
    private void handleStop() {
        isPlaying = false;
        currentPosition = 0;

        if (playButton != null) playButton.setDisable(false);
        if (pauseButton != null) pauseButton.setDisable(true);
        if (stopButton != null) stopButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Stopped");

        if (playbackTimer != null) {
            playbackTimer.stop();
        }

        drawTab();

        LOGGER.info("Playback stopped");
    }

    /**
     * Start playback animation timer
     */
    private void startPlaybackTimer() {
        if (playbackTimer != null) {
            playbackTimer.stop();
        }

        final long[] lastUpdateTime = {System.currentTimeMillis()};

        playbackTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPlaying || simulatedNotes == null) {
                    stop();
                    return;
                }

                long currentTime = System.currentTimeMillis();
                double speed = speedSlider != null ? speedSlider.getValue() / 100.0 : 1.0;
                long interval = (long) (500 / speed); // 500ms per note at 100% speed

                if (currentTime - lastUpdateTime[0] >= interval) {
                    currentPosition++;

                    if (currentPosition >= simulatedNotes.size()) {
                        handleStop();
                        return;
                    }

                    drawTab();
                    lastUpdateTime[0] = currentTime;
                }
            }
        };

        playbackTimer.start();
    }

    /**
     * Return to main menu
     */
    @FXML
    private void handleBackToMenu() {
        handleStop(); // Stop playback before leaving

        try {
            App.setRoot("mainmenu");
            LOGGER.info("Returning to main menu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main menu", e);
            DialogHelper.showError("Navigation Error", "Could not return to main menu");
        }
    }

    /**
     * Inner class representing a simulated note for playback
     */
    private static class SimulatedNote {
        private final int string;
        private final int fret;
        private final long timestamp;

        public SimulatedNote(int string, int fret, long timestamp) {
            this.string = string;
            this.fret = fret;
            this.timestamp = timestamp;
        }

        public int getString() {
            return string;
        }

        public int getFret() {
            return fret;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
