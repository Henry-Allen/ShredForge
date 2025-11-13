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
            DialogHelper.showError("Search Required", "Please enter a search query to find tabs.");
            return;
        }

        LOGGER.info("Searching for: " + query);
        showStatus("Searching...");

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Create loading dialog
        LoadingIndicator loader = new LoadingIndicator(
            "Searching Tabs",
            "Searching for \"" + query + "\"...",
            true
        );
        loader.show();

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

                    loader.hide();

                    if (results.isEmpty()) {
                        showStatus("No tabs found");
                        DialogHelper.showInfo(
                            "No Results",
                            "No tabs found for \"" + query + "\".\n\nTry a different search term or artist name."
                        );
                    } else {
                        showStatus("Found " + results.size() + " tabs");
                        LOGGER.info("Search complete: " + results.size() + " results");
                    }
                });

            } catch (Exception e) {
                LOGGER.severe("Search failed: " + e.getMessage());
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    loader.hide();
                    showStatus("Search failed");
                    DialogHelper.showErrorWithSuggestion(
                        "Search Failed",
                        "Could not search for tabs.",
                        "Check your internet connection and try again."
                    );
                });
            }
        }).start();
    }

    @FXML
    private void handleDownload() {
        Tab selectedTab = resultsTable.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            DialogHelper.showError(
                "No Tab Selected",
                "Please select a tab from the search results to download."
            );
            return;
        }

        LOGGER.info("Downloading tab: " + selectedTab.getTitle());
        showStatus("Downloading...");

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Create loading dialog with progress bar
        LoadingIndicator loader = new LoadingIndicator(
            "Downloading Tab",
            "Downloading \"" + selectedTab.getTitle() + "\" by " + selectedTab.getArtist(),
            false
        );
        loader.show();

        // Download in background thread
        new Thread(() -> {
            try {
                // Simulate progress updates
                Platform.runLater(() -> loader.updateProgress(0.3));
                Tab downloadedTab = tabManager.downloadTab(selectedTab);
                Platform.runLater(() -> loader.updateProgress(0.8));

                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }

                    loader.updateProgress(1.0);
                    loader.hide();

                    if (downloadedTab != null) {
                        showStatus("Downloaded: " + downloadedTab.getTitle());
                        LOGGER.info("Tab downloaded successfully");

                        // Show confirmation with DialogHelper
                        DialogHelper.showSuccess(
                            "Download Complete",
                            "\"" + downloadedTab.getTitle() + "\" downloaded successfully!\n\n" +
                            "You can now find it in 'My Tabs' and start practicing."
                        );
                    } else {
                        showStatus("Download failed");
                        DialogHelper.showError(
                            "Download Failed",
                            "Could not download the selected tab.\n\nPlease try again."
                        );
                    }
                });

            } catch (Exception e) {
                LOGGER.severe("Download failed: " + e.getMessage());
                Platform.runLater(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisible(false);
                    }
                    loader.hide();
                    showStatus("Download failed");
                    DialogHelper.showErrorWithSuggestion(
                        "Download Failed",
                        "Could not download the tab.",
                        "Check your internet connection and try again."
                    );
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
