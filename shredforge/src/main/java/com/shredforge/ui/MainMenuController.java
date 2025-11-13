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
    private Button exitButton;

    private final ShredForgeRepository repository;

    public MainMenuController() {
        this.repository = ShredForgeRepository.getInstance();
    }

    @FXML
    private void initialize() {
        LOGGER.info("Main Menu initialized");
        updateStatus();
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
        int count = repository.getTabCount();
        if (statusLabel != null) {
            if (count == 0) {
                statusLabel.setText("No tabs saved yet. Search and download tabs to get started!");
            } else {
                statusLabel.setText("You have " + count + " saved tab" + (count == 1 ? "" : "s"));
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
        if (statusLabel != null) {
            statusLabel.setText("Settings: Audio Device: " + repository.getAudioInputDevice());
        }
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
    }
}
