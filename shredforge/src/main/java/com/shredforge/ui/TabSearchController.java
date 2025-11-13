package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.Tab;
import com.shredforge.tab.TabManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.logging.Logger;

/**
 * Controller for Tab Search interface.
 * Allows users to search Songsterr for tabs and download them.
 * Per specification section 4.2.2
 */
public class TabSearchController {
    private static final Logger LOGGER = Logger.getLogger(TabSearchController.class.getName());

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> difficultyFilter;

    @FXML
    private Button searchButton;

    @FXML
    private TableView<Tab> resultsTable;

    @FXML
    private TableColumn<Tab, String> titleColumn;

    @FXML
    private TableColumn<Tab, String> artistColumn;

    @FXML
    private TableColumn<Tab, String> difficultyColumn;

    @FXML
    private TableColumn<Tab, Float> ratingColumn;

    @FXML
    private Button downloadButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    private final TabManager tabManager;
    private final ObservableList<Tab> searchResults;

    public TabSearchController() {
        this.tabManager = new TabManager();
        this.searchResults = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        LOGGER.info("Tab Search initialized");

        // Set up table columns
        if (titleColumn != null) {
            titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        }
        if (artistColumn != null) {
            artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        }
        if (difficultyColumn != null) {
            difficultyColumn.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        }
        if (ratingColumn != null) {
            ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        }

        if (resultsTable != null) {
            resultsTable.setItems(searchResults);
        }

        // Set up difficulty filter
        if (difficultyFilter != null) {
            difficultyFilter.setItems(FXCollections.observableArrayList(
                "All", "Easy", "Medium", "Hard", "Expert"
            ));
            difficultyFilter.setValue("All");
        }

        // Hide loading indicator initially
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();

        if (query == null || query.trim().isEmpty()) {
            showStatus("Please enter a search query");
            return;
        }

        LOGGER.info("Searching for: " + query);
        showStatus("Searching...");

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Perform search in background thread
        new Thread(() -> {
            try {
                String difficulty = difficultyFilter.getValue();
                List<Tab> results;

                if ("All".equals(difficulty)) {
                    results = tabManager.searchTabs(query);
                } else {
                    results = tabManager.searchTabs(query, difficulty, null);
                }

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    searchResults.clear();
                    searchResults.addAll(results);

                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }

                    showStatus("Found " + results.size() + " tabs");
                    LOGGER.info("Search complete: " + results.size() + " results");
                });

            } catch (Exception e) {
                LOGGER.severe("Search failed: " + e.getMessage());
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    showStatus("Search failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleDownload() {
        Tab selectedTab = resultsTable.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            showStatus("Please select a tab to download");
            return;
        }

        LOGGER.info("Downloading tab: " + selectedTab.getTitle());
        showStatus("Downloading...");

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Download in background thread
        new Thread(() -> {
            try {
                Tab downloadedTab = tabManager.downloadTab(selectedTab);

                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }

                    if (downloadedTab != null) {
                        showStatus("Downloaded: " + downloadedTab.getTitle());
                        LOGGER.info("Tab downloaded successfully");

                        // Show confirmation
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Download Complete");
                        alert.setHeaderText(null);
                        alert.setContentText("Tab downloaded successfully!\n\n" +
                                           "You can now practice this tab.");
                        alert.showAndWait();
                    } else {
                        showStatus("Download failed");
                    }
                });

            } catch (Exception e) {
                LOGGER.severe("Download failed: " + e.getMessage());
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    showStatus("Download failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
