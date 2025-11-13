package com.shredforge.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tracks recently accessed tabs for quick access.
 * Maintains a limited history of tab IDs in most-recently-used order.
 */
public class RecentTabsTracker {
    private static final Logger LOGGER = Logger.getLogger(RecentTabsTracker.class.getName());
    private static final int MAX_RECENT_TABS = 10;

    private final LinkedList<String> recentTabIds;
    private final int maxSize;

    /**
     * Create tracker with default size
     */
    public RecentTabsTracker() {
        this(MAX_RECENT_TABS);
    }

    /**
     * Create tracker with custom size
     * @param maxSize Maximum number of recent tabs to track
     */
    public RecentTabsTracker(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.recentTabIds = new LinkedList<>();
        LOGGER.info("Recent tabs tracker initialized with max size: " + this.maxSize);
    }

    /**
     * Add a tab to recent history
     * @param tabId Tab identifier
     */
    public synchronized void addTab(String tabId) {
        if (tabId == null || tabId.trim().isEmpty()) {
            LOGGER.warning("Cannot add null or empty tab ID to recent history");
            return;
        }

        // Remove existing occurrence if present
        recentTabIds.remove(tabId);

        // Add to front (most recent)
        recentTabIds.addFirst(tabId);

        // Trim to max size
        while (recentTabIds.size() > maxSize) {
            String removed = recentTabIds.removeLast();
            LOGGER.fine("Removed old tab from recent history: " + removed);
        }

        LOGGER.fine("Added tab to recent history: " + tabId);
    }

    /**
     * Get list of recent tab IDs in order (most recent first)
     * @return Immutable list of recent tab IDs
     */
    public synchronized List<String> getRecentTabs() {
        return new ArrayList<>(recentTabIds);
    }

    /**
     * Get the most recently accessed tab ID
     * @return Most recent tab ID, or null if none
     */
    public synchronized String getMostRecent() {
        return recentTabIds.isEmpty() ? null : recentTabIds.getFirst();
    }

    /**
     * Check if a tab is in recent history
     * @param tabId Tab identifier
     * @return True if tab is in recent history
     */
    public synchronized boolean contains(String tabId) {
        return recentTabIds.contains(tabId);
    }

    /**
     * Remove a tab from recent history
     * @param tabId Tab identifier
     */
    public synchronized void removeTab(String tabId) {
        if (recentTabIds.remove(tabId)) {
            LOGGER.fine("Removed tab from recent history: " + tabId);
        }
    }

    /**
     * Clear all recent history
     */
    public synchronized void clear() {
        recentTabIds.clear();
        LOGGER.info("Cleared recent tabs history");
    }

    /**
     * Get number of tabs in recent history
     */
    public synchronized int size() {
        return recentTabIds.size();
    }

    /**
     * Check if recent history is empty
     */
    public synchronized boolean isEmpty() {
        return recentTabIds.isEmpty();
    }

    /**
     * Load recent tabs from list (for persistence)
     * @param tabIds List of tab IDs in order
     */
    public synchronized void loadFromList(List<String> tabIds) {
        if (tabIds == null) {
            LOGGER.warning("Cannot load null tab list");
            return;
        }

        recentTabIds.clear();
        for (String tabId : tabIds) {
            if (tabId != null && !tabId.trim().isEmpty()) {
                recentTabIds.add(tabId);
                if (recentTabIds.size() >= maxSize) {
                    break;
                }
            }
        }

        LOGGER.info("Loaded " + recentTabIds.size() + " recent tabs from persistence");
    }

    /**
     * Convert to list for persistence
     * @return List of tab IDs in order
     */
    public synchronized List<String> toList() {
        return new ArrayList<>(recentTabIds);
    }
}
