package com.shredforge.ui;

import com.shredforge.repository.ShredForgeRepository;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.logging.Logger;

/**
 * Controller for the main menu interface.
 * Provides access to all main features: Tab Search, My Tabs, Calibration, and Settings.
 */
public class MainMenuController {
    private static final Logger LOGGER = Logger.getLogger(MainMenuController.class.getName());

    @FXML
    private VBox mainContainer;

    @FXML
    private Label titleLabel;

    @FXML
    private Button searchTabsButton;

    @FXML
    private Button myTabsButton;

    @FXML
    private Button calibrateButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label tabCountLabel;

    @FXML
    private Label calibrationStatusLabel;

    @FXML
    private Button helpButton;

    @FXML
    private Button exitButton;

    @FXML
    private VBox recentTabsContainer;

    @FXML
    private Label recentTabsHintLabel;

    @FXML
    private VBox recentTabsList;

    @FXML
    private Button quickPracticeButton;

    private final ShredForgeRepository repository;

    public MainMenuController() {
        this.repository = ShredForgeRepository.getInstance();
    }

    @FXML
    private void initialize() {
        LOGGER.info("Main Menu initialized");
        updateStatus();
        updateRecentTabs();
    }

    /**
     * Update status labels
     */
    private void updateStatus() {
        Platform.runLater(() -> {
            if (tabCountLabel != null) {
                int tabCount = repository.getTabCount();
                tabCountLabel.setText("Saved Tabs: " + tabCount);
            }

            if (calibrationStatusLabel != null) {
                boolean calibrated = repository.isCalibrated();
                calibrationStatusLabel.setText("Calibration: " + (calibrated ? "✓ Complete" : "⚠ Required"));
                calibrationStatusLabel.setStyle(calibrated ?
                    "-fx-text-fill: green;" : "-fx-text-fill: orange;");
            }

            if (statusLabel != null) {
                statusLabel.setText("Ready");
            }
        });
    }

    @FXML
    private void handleSearchTabs() {
        LOGGER.info("Search Tabs clicked");
        try {
            com.shredforge.App.setRoot("tabsearch");
        } catch (Exception e) {
            LOGGER.severe("Failed to load tab search: " + e.getMessage());
            if (statusLabel != null) {
                statusLabel.setText("Error loading tab search");
            }
        }
    }

    @FXML
    private void handleMyTabs() {
        LOGGER.info("My Tabs clicked");
        try {
            com.shredforge.App.setRoot("mytabs");
        } catch (Exception e) {
            LOGGER.severe("Failed to load my tabs: " + e.getMessage());
            if (statusLabel != null) {
                statusLabel.setText("Error loading my tabs");
            }
        }
    }

    @FXML
    private void handleCalibrate() {
        LOGGER.info("Calibrate clicked");
        try {
            com.shredforge.App.setRoot("calibration");
        } catch (Exception e) {
            LOGGER.severe("Failed to load calibration: " + e.getMessage());
            if (statusLabel != null) {
                statusLabel.setText("Error loading calibration");
            }
        }
    }

    @FXML
    private void handleSettings() {
        LOGGER.info("Settings clicked");
        try {
            com.shredforge.App.setRoot("settings");
        } catch (Exception e) {
            LOGGER.severe("Failed to load settings: " + e.getMessage());
            if (statusLabel != null) {
                statusLabel.setText("Error loading settings");
            }
        }
    }

    @FXML
    private void handleHelp() {
        LOGGER.info("Help clicked");
        DialogHelper.showKeyboardShortcuts();
    }

    @FXML
    private void handleExit() {
        LOGGER.info("Exit clicked");
        Platform.exit();
        System.exit(0);
    }

    /**
     * Refresh the status display
     */
    public void refresh() {
        updateStatus();
        updateRecentTabs();
    }

    /**
     * Update recent tabs display
     */
    private void updateRecentTabs() {
        Platform.runLater(() -> {
            if (recentTabsList == null) {
                return;
            }

            // Clear existing items
            recentTabsList.getChildren().clear();

            // Get recent tabs from repository
            java.util.List<com.shredforge.model.Tab> recentTabs = repository.getRecentTabs();

            if (recentTabs.isEmpty()) {
                // Show hint if no recent tabs
                if (recentTabsHintLabel != null) {
                    recentTabsHintLabel.setVisible(true);
                }
                if (quickPracticeButton != null) {
                    quickPracticeButton.setVisible(false);
                }
            } else {
                // Hide hint
                if (recentTabsHintLabel != null) {
                    recentTabsHintLabel.setVisible(false);
                }

                // Show quick practice button
                if (quickPracticeButton != null) {
                    quickPracticeButton.setVisible(true);
                }

                // Display up to 3 most recent tabs
                int displayCount = Math.min(3, recentTabs.size());
                for (int i = 0; i < displayCount; i++) {
                    com.shredforge.model.Tab tab = recentTabs.get(i);
                    addRecentTabItem(tab);
                }
            }
        });
    }

    /**
     * Add a recent tab item to the display
     */
    private void addRecentTabItem(com.shredforge.model.Tab tab) {
        HBox itemBox = new HBox(10);
        itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemBox.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 5;");
        itemBox.setMaxWidth(Double.MAX_VALUE);

        // Tab info
        VBox infoBox = new VBox(2);
        Label titleLabel = new Label(tab.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label artistLabel = new Label(tab.getArtist());
        artistLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        infoBox.getChildren().addAll(titleLabel, artistLabel);

        // Practice button
        Button practiceBtn = new Button("Practice");
        practiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px;");
        practiceBtn.setOnAction(e -> launchPracticeSession(tab));

        // Add spacer to push button to right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        itemBox.getChildren().addAll(infoBox, spacer, practiceBtn);
        recentTabsList.getChildren().add(itemBox);
    }

    /**
     * Handle quick practice button (resume last session)
     */
    @FXML
    private void handleQuickPractice() {
        LOGGER.info("Quick Practice clicked");
        com.shredforge.model.Tab mostRecent = repository.getMostRecentTab();
        if (mostRecent != null) {
            launchPracticeSession(mostRecent);
        } else {
            if (statusLabel != null) {
                statusLabel.setText("No recent practice sessions found");
            }
        }
    }

    /**
     * Launch practice session for a tab
     */
    private void launchPracticeSession(com.shredforge.model.Tab tab) {
        try {
            LOGGER.info("Launching practice session for: " + tab.getTitle());

            // Check if calibrated
            if (!repository.isCalibrated()) {
                boolean proceed = DialogHelper.showConfirmation(
                    "Calibration Required",
                    "Your guitar input hasn't been calibrated yet.\n\n" +
                    "Would you like to calibrate now? (Recommended for best accuracy)"
                );

                if (proceed) {
                    com.shredforge.App.setRoot("calibration");
                    return;
                }
            }

            // Set the current tab in repository so practice session can access it
            repository.setCurrentTab(tab);

            // Navigate to practice session
            com.shredforge.App.setRoot("practicesession");

        } catch (Exception e) {
            LOGGER.severe("Failed to launch practice session: " + e.getMessage());
            DialogHelper.showError(
                "Launch Failed",
                "Could not start practice session.\n\nPlease try again."
            );
        }
    }
}
