package com.shredforge.tab;

import com.shredforge.model.Tab;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Service for saving tabs to local filesystem.
 * Handles file I/O and JSON serialization.
 */
public class TabSaveService {
    private static final Logger LOGGER = Logger.getLogger(TabSaveService.class.getName());
    private static final String TABS_DIRECTORY = System.getProperty("user.home") + "/.shredforge/tabs";

    private final ObjectMapper objectMapper;
    private final Path saveDirectory;

    public TabSaveService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.saveDirectory = Paths.get(TABS_DIRECTORY);

        // Create tabs directory if it doesn't exist
        try {
            Files.createDirectories(saveDirectory);
            LOGGER.info("Tabs directory: " + saveDirectory);
        } catch (IOException e) {
            LOGGER.severe("Failed to create tabs directory: " + e.getMessage());
        }
    }

    /**
     * Save tab to local file system as JSON
     */
    public boolean saveToFile(Tab tab) {
        if (tab == null || tab.getId() == null) {
            LOGGER.warning("Cannot save null tab");
            return false;
        }

        try {
            // Validate tab before saving
            if (!validateTab(tab)) {
                LOGGER.warning("Tab validation failed");
                return false;
            }

            File file = getTabFile(tab.getId());

            // Check if file already exists
            if (file.exists()) {
                LOGGER.info("Tab file already exists, overwriting: " + file.getName());
            }

            objectMapper.writeValue(file, tab);
            tab.setLocalPath(file.getAbsolutePath());

            LOGGER.info("Tab saved to file: " + file.getName());
            return true;

        } catch (IOException e) {
            LOGGER.severe("Failed to save tab: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load tab from local file
     */
    public Tab loadFromFile(String tabId) {
        try {
            File file = getTabFile(tabId);

            if (!file.exists()) {
                LOGGER.warning("Tab file not found: " + tabId);
                return null;
            }

            Tab tab = objectMapper.readValue(file, Tab.class);
            tab.setLocalPath(file.getAbsolutePath());
            tab.setDownloaded(true);

            LOGGER.info("Tab loaded from file: " + file.getName());
            return tab;

        } catch (IOException e) {
            LOGGER.severe("Failed to load tab: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete tab file
     */
    public boolean deleteFile(String tabId) {
        try {
            File file = getTabFile(tabId);

            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    LOGGER.info("Tab file deleted: " + file.getName());
                }
                return deleted;
            } else {
                LOGGER.warning("Tab file not found for deletion: " + tabId);
                return false;
            }

        } catch (Exception e) {
            LOGGER.severe("Failed to delete tab file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate tab before saving
     */
    public boolean validateTab(Tab tab) {
        if (tab.getId() == null || tab.getId().isEmpty()) {
            LOGGER.warning("Tab ID is required");
            return false;
        }

        if (tab.getTitle() == null || tab.getTitle().isEmpty()) {
            LOGGER.warning("Tab title is required");
            return false;
        }

        return true;
    }

    /**
     * Get file for a tab ID
     */
    private File getTabFile(String tabId) {
        // Sanitize filename
        String sanitized = tabId.replaceAll("[^a-zA-Z0-9.-]", "_");
        return saveDirectory.resolve(sanitized + ".json").toFile();
    }

    /**
     * Get save directory path
     */
    public Path getSaveDirectory() {
        return saveDirectory;
    }

    /**
     * Check if a tab file exists
     */
    public boolean fileExists(String tabId) {
        return getTabFile(tabId).exists();
    }
}
