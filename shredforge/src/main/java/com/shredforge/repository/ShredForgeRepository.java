package com.shredforge.repository;

import com.shredforge.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central data repository for ShredForge following the Repository architectural pattern.
 * Provides unified data access for all subsystems including tabs, calibration, sessions, and settings.
 * Thread-safe singleton implementation.
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

    private ShredForgeRepository() {
        this.tabs = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
        this.calibrationData = new CalibrationData();
        this.currentSession = null;
        this.currentTab = null;

        this.audioInputDevice = "default";
        this.isCalibrated = false;
        this.masterVolume = 0.8f;

        LOGGER.info("ShredForgeRepository initialized");
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
     * Save calibration data
     */
    public void saveCalibrationData(CalibrationData data) {
        if (data == null) {
            throw new IllegalArgumentException("Calibration data cannot be null");
        }
        this.calibrationData = data;
        this.isCalibrated = data.isCalibrated();
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
     * Set audio input device
     */
    public void setAudioInputDevice(String device) {
        this.audioInputDevice = device;
        LOGGER.info("Audio input device set to: " + device);
    }

    /**
     * Get audio input device
     */
    public String getAudioInputDevice() {
        return audioInputDevice;
    }

    /**
     * Set master volume
     */
    public void setMasterVolume(float volume) {
        if (volume < 0.0f || volume > 1.0f) {
            throw new IllegalArgumentException("Volume must be between 0.0 and 1.0");
        }
        this.masterVolume = volume;
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
        LOGGER.warning("All repository data cleared");
    }
}
