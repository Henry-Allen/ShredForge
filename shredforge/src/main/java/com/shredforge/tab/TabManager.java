package com.shredforge.tab;

import com.shredforge.model.Tab;
import com.shredforge.repository.ShredForgeRepository;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages tab operations including loading, saving, and searching.
 * Coordinates between TabGetService, TabSaveService, and Repository.
 */
public class TabManager {
    private static final Logger LOGGER = Logger.getLogger(TabManager.class.getName());

    private final ShredForgeRepository repository;
    private final TabGetService tabGetService;
    private final TabSaveService tabSaveService;
    private Tab currentTab;

    public TabManager() {
        this.repository = ShredForgeRepository.getInstance();
        this.tabGetService = new TabGetService();
        this.tabSaveService = new TabSaveService();
    }

    /**
     * Search for tabs online using Songsterr API
     * @param query Search query (song title, artist, etc.)
     * @return List of matching tabs
     */
    public List<Tab> searchTabs(String query) {
        if (query == null || query.trim().isEmpty()) {
            LOGGER.warning("Empty search query");
            return List.of();
        }

        try {
            LOGGER.info("Searching tabs for: " + query);
            return tabGetService.searchOnline(query);
        } catch (Exception e) {
            LOGGER.severe("Tab search failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Search tabs with filters
     */
    public List<Tab> searchTabs(String query, String difficulty, Integer minRating) {
        List<Tab> results = searchTabs(query);

        // Apply filters
        if (difficulty != null && !difficulty.isEmpty()) {
            results = results.stream()
                .filter(tab -> difficulty.equals(tab.getDifficulty()))
                .collect(Collectors.toList());
        }

        if (minRating != null) {
            results = results.stream()
                .filter(tab -> tab.getRating() >= minRating)
                .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Download and save a tab
     * @param tab Tab to download (can be partial metadata from search)
     * @return Complete tab with full details
     */
    public Tab downloadTab(Tab tab) {
        if (tab == null || tab.getId() == null) {
            LOGGER.warning("Cannot download null tab");
            return null;
        }

        try {
            // Check if already downloaded
            if (repository.isTabDownloaded(tab.getId())) {
                LOGGER.info("Tab already downloaded: " + tab.getTitle());
                return repository.getTab(tab.getId());
            }

            // Download full tab details
            LOGGER.info("Downloading tab: " + tab.getTitle());
            Tab fullTab = tabGetService.downloadTab(tab.getId());

            if (fullTab != null) {
                // Save to local storage
                tabSaveService.saveToFile(fullTab);
                // Save to repository
                repository.saveTab(fullTab);
                fullTab.setDownloaded(true);
                LOGGER.info("Tab downloaded successfully: " + fullTab.getTitle());
                return fullTab;
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to download tab: " + e.getMessage());
        }

        return null;
    }

    /**
     * Load a tab from local storage
     */
    public Tab loadTab(String tabId) {
        Tab tab = repository.getTab(tabId);
        if (tab != null) {
            currentTab = tab;
            repository.setCurrentTab(tab);
            LOGGER.info("Tab loaded: " + tab.getTitle());
        } else {
            LOGGER.warning("Tab not found: " + tabId);
        }
        return tab;
    }

    /**
     * Get all locally saved tabs
     */
    public List<Tab> getLocalTabs() {
        return repository.getAllTabs();
    }

    /**
     * Delete a tab from local storage
     */
    public boolean deleteTab(String tabId) {
        boolean deleted = repository.deleteTab(tabId);
        if (deleted) {
            tabSaveService.deleteFile(tabId);
            LOGGER.info("Tab deleted: " + tabId);
        }
        return deleted;
    }

    /**
     * Get current tab
     */
    public Tab getCurrentTab() {
        return currentTab;
    }

    /**
     * Set current tab
     */
    public void setCurrentTab(Tab tab) {
        this.currentTab = tab;
        repository.setCurrentTab(tab);
    }
}
