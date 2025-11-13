package com.shredforge.util;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Throttles UI updates to prevent excessive JavaFX thread usage and stuttering.
 * Ensures UI updates happen at most once per time window.
 */
public class UIThrottler {
    private static final Logger LOGGER = Logger.getLogger(UIThrottler.class.getName());

    private final long throttleIntervalMs;
    private long lastUpdateTime;
    private final AtomicBoolean updateScheduled;
    private Runnable pendingUpdate;

    /**
     * Create throttler with default 16ms interval (~60 FPS)
     */
    public UIThrottler() {
        this(16);
    }

    /**
     * Create throttler with custom interval
     * @param throttleIntervalMs Minimum milliseconds between updates
     */
    public UIThrottler(long throttleIntervalMs) {
        this.throttleIntervalMs = throttleIntervalMs;
        this.lastUpdateTime = 0;
        this.updateScheduled = new AtomicBoolean(false);
        this.pendingUpdate = null;
    }

    /**
     * Request UI update (will be throttled)
     * @param updateTask Task to run on JavaFX thread
     */
    public void requestUpdate(Runnable updateTask) {
        if (updateTask == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastUpdateTime;

        if (timeSinceLastUpdate >= throttleIntervalMs) {
            // Enough time has passed, update immediately
            lastUpdateTime = currentTime;
            Platform.runLater(updateTask);
        } else {
            // Too soon, schedule for later
            synchronized (this) {
                pendingUpdate = updateTask;

                if (!updateScheduled.get()) {
                    updateScheduled.set(true);
                    long delay = throttleIntervalMs - timeSinceLastUpdate;

                    ThreadPoolManager.getInstance().schedule(() -> {
                        Runnable task;
                        synchronized (UIThrottler.this) {
                            task = pendingUpdate;
                            pendingUpdate = null;
                            updateScheduled.set(false);
                            lastUpdateTime = System.currentTimeMillis();
                        }

                        if (task != null) {
                            Platform.runLater(task);
                        }
                    }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Force immediate update (bypass throttling)
     * @param updateTask Task to run on JavaFX thread
     */
    public void forceUpdate(Runnable updateTask) {
        if (updateTask == null) {
            return;
        }

        synchronized (this) {
            pendingUpdate = null;
            updateScheduled.set(false);
            lastUpdateTime = System.currentTimeMillis();
        }

        Platform.runLater(updateTask);
    }

    /**
     * Cancel any pending updates
     */
    public void cancel() {
        synchronized (this) {
            pendingUpdate = null;
            updateScheduled.set(false);
        }
    }

    /**
     * Get throttle interval
     */
    public long getThrottleInterval() {
        return throttleIntervalMs;
    }
}
