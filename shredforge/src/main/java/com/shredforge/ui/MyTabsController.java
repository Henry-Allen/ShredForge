package com.shredforge.ui;

import com.shredforge.App;
import com.shredforge.model.Tab;
import com.shredforge.repository.ShredForgeRepository;
import com.shredforge.tab.TabManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for My Tabs interface.
 * Displays and manages locally saved tabs.
 */
public class MyTabsController {
    private static final Logger LOGGER = Logger.getLogger(MyTabsController.class.getName());

    @FXML
    private TableView<Tab> tabsTable;

    @FXML
    private TableColumn<Tab, String> titleColumn;

    @FXML
    private TableColumn<Tab, String> artistColumn;

    @FXML
    private TableColumn<Tab, String> difficultyColumn;

    @FXML
    private TableColumn<Tab, Integer> tempoColumn;

    @FXML
    private Button practiceButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label tabCountLabel;

    private final ShredForgeRepository repository;
    private final TabManager tabManager;
    private final ObservableList<Tab> tabs;

    public MyTabsController() {
        this.repository = ShredForgeRepository.getInstance();
        this.tabManager = new TabManager();
        this.tabs = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        LOGGER.info("My Tabs initialized");

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
        if (tempoColumn != null) {
            tempoColumn.setCellValueFactory(new PropertyValueFactory<>("tempo"));
        }

        if (tabsTable != null) {
            tabsTable.setItems(tabs);
        }

        // Load tabs
        loadTabs();

        // Initial button state
        updateButtonStates();

        // Add selection listener
        if (tabsTable != null) {
            tabsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonStates()
            );
        }
    }

    private void loadTabs() {
        tabs.clear();
        List<Tab> savedTabs = tabManager.getLocalTabs();
        tabs.addAll(savedTabs);

        if (tabCountLabel != null) {
            tabCountLabel.setText("Total Tabs: " + tabs.size());
        }

        if (tabs.isEmpty()) {
            showStatus("No tabs saved yet. Use Search Tabs to download some!");
        } else {
            showStatus(tabs.size() + " tab" + (tabs.size() == 1 ? "" : "s") + " available");
        }

        LOGGER.info("Loaded " + tabs.size() + " tabs");
    }

    @FXML
    private void handlePractice() {
        Tab selectedTab = tabsTable.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            showStatus("Please select a tab to practice");
            return;
        }

        LOGGER.info("Starting practice with tab: " + selectedTab.getTitle());

        // Set as current tab in repository
        repository.setCurrentTab(selectedTab);

        // Navigate to practice session
        try {
            App.setRoot("practicesession");
        } catch (Exception e) {
            LOGGER.severe("Failed to start practice session: " + e.getMessage());
            showStatus("Error starting practice session");
        }
    }

    @FXML
    private void handleDelete() {
        Tab selectedTab = tabsTable.getSelectionModel().getSelectedItem();

        if (selectedTab == null) {
            showStatus("Please select a tab to delete");
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Tab");
        alert.setHeaderText("Delete " + selectedTab.getTitle() + "?");
        alert.setContentText("This will permanently delete this tab from your library.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Delete the tab
            boolean deleted = tabManager.deleteTab(selectedTab.getId());

            if (deleted) {
                tabs.remove(selectedTab);
                showStatus("Tab deleted: " + selectedTab.getTitle());
                LOGGER.info("Tab deleted: " + selectedTab.getTitle());

                if (tabCountLabel != null) {
                    tabCountLabel.setText("Total Tabs: " + tabs.size());
                }
            } else {
                showStatus("Failed to delete tab");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        LOGGER.info("Refreshing tab list");
        loadTabs();
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("mainmenu");
        } catch (Exception e) {
            LOGGER.severe("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = tabsTable != null &&
                              tabsTable.getSelectionModel().getSelectedItem() != null;

        if (practiceButton != null) {
            practiceButton.setDisable(!hasSelection);
        }

        if (deleteButton != null) {
            deleteButton.setDisable(!hasSelection);
        }
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
