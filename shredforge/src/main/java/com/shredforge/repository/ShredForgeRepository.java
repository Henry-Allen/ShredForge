package com.shredforge.repository;

import com.shredforge.model.*;
import com.shredforge.persistence.DataPersistence;
import com.shredforge.util.RecentTabsTracker;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central data repository for ShredForge following the Repository architectural pattern.
 * Provides unified data access for all subsystems including tabs, calibration, sessions, and settings.
 * Thread-safe singleton implementation with persistence.
 */
public class ShredForgeRepository {
    private static final Logger LOGGER = Logger.getLogger(ShredForgeRepository.class.getName());
    private static ShredForgeRepository instance;

    // Data stores
    private final Map<String, Tab> tabs;  // Tab ID -> Tab
    private final Map<String, Session> sessions;  // Session ID -> Session
    private CalibrationData calibrationData;
    private Session currentSession;
    private Tab currentTab;

    // Settings
    private String audioInputDevice;
    private boolean isCalibrated;
    private float masterVolume;
    private String theme;

    // Persistence and utilities
    private final DataPersistence persistence;
    private final RecentTabsTracker recentTabsTracker;

    private ShredForgeRepository() {
        this.tabs = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
        this.persistence = new DataPersistence();
        this.recentTabsTracker = new RecentTabsTracker();

        // Load persisted data
        loadPersistedData();

        LOGGER.info("ShredForgeRepository initialized");
    }

    /**
     * Load persisted data from disk
     */
    private void loadPersistedData() {
        // Load calibration data
        CalibrationData savedCalibration = persistence.loadCalibrationData();
        if (savedCalibration != null) {
            this.calibrationData = savedCalibration;
            this.isCalibrated = savedCalibration.isCalibrated();
            LOGGER.info("Loaded calibration data from disk");
        } else {
            this.calibrationData = new CalibrationData();
            this.isCalibrated = false;
        }

        // Load settings
        DataPersistence.AppSettings settings = persistence.loadSettings();
        this.audioInputDevice = settings.audioInputDevice;
        this.masterVolume = settings.masterVolume;
        this.theme = settings.theme != null ? settings.theme : "light";

        // Load recent tabs
        if (settings.recentTabs != null && !settings.recentTabs.isEmpty()) {
            this.recentTabsTracker.loadFromList(settings.recentTabs);
            LOGGER.info("Loaded " + settings.recentTabs.size() + " recent tabs from settings");
        }

        LOGGER.info("Loaded settings from disk (theme: " + this.theme + ")");

        this.currentSession = null;
        this.currentTab = null;
    }

    /**
     * Get singleton instance of the repository
     */
    public static synchronized ShredForgeRepository getInstance() {
        if (instance == null) {
            instance = new ShredForgeRepository();
        }
        return instance;
    }

    // ========== Tab Management ==========

    /**
     * Save a tab to the repository
     */
    public void saveTab(Tab tab) {
        if (tab == null || tab.getId() == null) {
            throw new IllegalArgumentException("Tab and tab ID cannot be null");
        }
        tabs.put(tab.getId(), tab);
        LOGGER.info("Tab saved: " + tab.getTitle() + " by " + tab.getArtist());
    }

    /**
     * Get a tab by ID
     */
    public Tab getTab(String id) {
        return tabs.get(id);
    }

    /**
     * Get all saved tabs
     */
    public List<Tab> getAllTabs() {
        return new ArrayList<>(tabs.values());
    }

    /**
     * Delete a tab
     */
    public boolean deleteTab(String id) {
        Tab removed = tabs.remove(id);
        if (removed != null) {
            LOGGER.info("Tab deleted: " + removed.getTitle());
            return true;
        }
        return false;
    }

    /**
     * Check if a tab is already downloaded
     */
    public boolean isTabDownloaded(String id) {
        return tabs.containsKey(id);
    }

    /**
     * Get count of saved tabs
     */
    public int getTabCount() {
        return tabs.size();
    }

    // ========== Session Management ==========

    /**
     * Create a new practice session
     */
    public Session createSession(Tab tab) {
        if (tab == null) {
            throw new IllegalArgumentException("Tab cannot be null");
        }
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, tab);
        sessions.put(sessionId, session);
        currentSession = session;
        currentTab = tab;

        // Track this tab in recent history
        recentTabsTracker.addTab(tab.getId());
        saveSettings(); // Save recent tabs to disk

        LOGGER.info("Session created for tab: " + tab.getTitle());
        return session;
    }

    /**
     * Get current active session
     */
    public Session getCurrentSession() {
        return currentSession;
    }

    /**
     * End current session
     */
    public void endSession() {
        if (currentSession != null) {
            currentSession.complete();
            LOGGER.info("Session ended: " + currentSession.getSessionId());
            currentSession = null;
        }
    }

    /**
     * Save a score report
     */
    public void saveScoreReport(ScoreReport report) {
        if (report != null) {
            LOGGER.info("Score report saved: " + report.getGrade() +
                       " (" + report.getAccuracyPercentage() + "%)");
        }
    }

    /**
     * Get all sessions
     */
    public List<Session> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    // ========== Calibration Management ==========

    /**
     * Save calibration data (in memory and to disk)
     */
    public void saveCalibrationData(CalibrationData data) {
        if (data == null) {
            throw new IllegalArgumentException("Calibration data cannot be null");
        }
        this.calibrationData = data;
        this.isCalibrated = data.isCalibrated();

        // Persist to disk
        persistence.saveCalibrationData(data);

        LOGGER.info("Calibration data saved: " + data.toString());
    }

    /**
     * Get calibration data
     */
    public CalibrationData getCalibrationData() {
        return calibrationData;
    }

    /**
     * Check if system is calibrated
     */
    public boolean isCalibrated() {
        return isCalibrated && calibrationData.isComplete();
    }

    /**
     * Reset calibration
     */
    public void resetCalibration() {
        calibrationData.reset();
        isCalibrated = false;
        LOGGER.info("Calibration reset");
    }

    // ========== Settings Management ==========

    /**
     * Set audio input device (saves to disk)
     */
    public void setAudioInputDevice(String device) {
        this.audioInputDevice = device;
        saveSettings();
        LOGGER.info("Audio input device set to: " + device);
    }

    /**
     * Get audio input device
     */
    public String getAudioInputDevice() {
        return audioInputDevice;
    }

    /**
     * Set master volume (saves to disk)
     */
    public void setMasterVolume(float volume) {
        if (volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");
        }
        this.masterVolume = volume;
        saveSettings();
    }

    /**
     * Save current settings to disk
     */
    private void saveSettings() {
        DataPersistence.AppSettings settings = new DataPersistence.AppSettings();
        settings.audioInputDevice = this.audioInputDevice;
        settings.masterVolume = this.masterVolume;
        settings.theme = this.theme;
        settings.recentTabs = recentTabsTracker.toList();
        if (!recentTabsTracker.isEmpty()) {
            settings.lastPracticedTab = recentTabsTracker.getMostRecent();
        }
        persistence.saveSettings(settings);
    }

    /**
     * Get master volume
     */
    public float getMasterVolume() {
        return masterVolume;
    }

    // ========== Current State Management ==========

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
        LOGGER.info("Current tab set to: " + (tab != null ? tab.getTitle() : "null"));
    }

    /**
     * Get system status summary
     */
    public String getSystemStatus() {
        return String.format(
            "ShredForge Status:\n" +
            "- Tabs: %d\n" +
            "- Calibrated: %s\n" +
            "- Current Tab: %s\n" +
            "- Active Session: %s\n" +
            "- Audio Device: %s",
            tabs.size(),
            isCalibrated ? "Yes" : "No",
            currentTab != null ? currentTab.getTitle() : "None",
            currentSession != null ? "Yes" : "No",
            audioInputDevice
        );
    }

    /**
     * Clear all data (for testing or reset)
     */
    public void clearAll() {
        tabs.clear();
        sessions.clear();
        calibrationData.reset();
        currentSession = null;
        currentTab = null;
        isCalibrated = false;
        recentTabsTracker.clear();
        LOGGER.warning("All repository data cleared");
    }

    // ========== Theme Management ==========

    /**
     * Get current theme
     * @return "light" or "dark"
     */
    public String getTheme() {
        return theme != null ? theme : "light";
    }

    /**
     * Set theme and save to disk
     * @param theme "light" or "dark"
     */
    public void setTheme(String theme) {
        if (theme == null || (!theme.equals("light") && !theme.equals("dark"))) {
            LOGGER.warning("Invalid theme: " + theme + ", defaulting to light");
            this.theme = "light";
        } else {
            this.theme = theme;
        }
        saveSettings();
        LOGGER.info("Theme set to: " + this.theme);
    }

    /**
     * Toggle between light and dark theme
     * @return New theme value
     */
    public String toggleTheme() {
        if ("dark".equals(theme)) {
            setTheme("light");
        } else {
            setTheme("dark");
        }
        return theme;
    }

    // ========== Recent Tabs Management ==========

    /**
     * Get list of recent tab IDs
     * @return List of recent tab IDs in order (most recent first)
     */
    public List<String> getRecentTabIds() {
        return recentTabsTracker.getRecentTabs();
    }

    /**
     * Get list of recent tabs (full Tab objects)
     * @return List of recent Tab objects
     */
    public List<Tab> getRecentTabs() {
        List<Tab> recentTabs = new ArrayList<>();
        for (String tabId : recentTabsTracker.getRecentTabs()) {
            Tab tab = tabs.get(tabId);
            if (tab != null) {
                recentTabs.add(tab);
            } else {
                // Tab was deleted, remove from recent history
                recentTabsTracker.removeTab(tabId);
            }
        }
        return recentTabs;
    }

    /**
     * Get the most recently practiced tab
     * @return Most recent tab, or null if none
     */
    public Tab getMostRecentTab() {
        String recentId = recentTabsTracker.getMostRecent();
        if (recentId != null) {
            Tab tab = tabs.get(recentId);
            if (tab == null) {
                // Tab was deleted, remove from history
                recentTabsTracker.removeTab(recentId);
            }
            return tab;
        }
        return null;
    }
}
