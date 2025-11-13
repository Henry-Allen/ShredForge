package com.shredforge.util;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Centralized thread pool manager for background operations.
 * Provides optimized thread pools for different types of tasks.
 */
public class ThreadPoolManager {
    private static final Logger LOGGER = Logger.getLogger(ThreadPoolManager.class.getName());
    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    // Thread pools for different task types
    private final ExecutorService audioProcessingPool;
    private final ExecutorService networkOperationsPool;
    private final ScheduledExecutorService scheduledTasksPool;

    private ThreadPoolManager() {
        // Audio processing: fixed size pool (2 threads for real-time processing)
        this.audioProcessingPool = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AudioProcessing-" + counter++);
                t.setDaemon(true);
                t.setPriority(Thread.MAX_PRIORITY - 1); // High priority for audio
                return t;
            }
        });

        // Network operations: cached pool (creates threads as needed)
        this.networkOperationsPool = Executors.newCachedThreadPool(new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NetworkOperation-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });

        // Scheduled tasks: 2 threads for periodic operations
        this.scheduledTasksPool = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ScheduledTask-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });

        LOGGER.info("Thread pool manager initialized");
    }

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Submit audio processing task
     */
    public Future<?> submitAudioTask(Runnable task) {
        return audioProcessingPool.submit(task);
    }

    /**
     * Submit network operation task
     */
    public <T> Future<T> submitNetworkTask(Callable<T> task) {
        return networkOperationsPool.submit(task);
    }

    /**
     * Schedule periodic task
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledTasksPool.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Schedule one-time delayed task
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledTasksPool.schedule(task, delay, unit);
    }

    /**
     * Shutdown all thread pools gracefully
     */
    public void shutdown() {
        LOGGER.info("Shutting down thread pools");

        audioProcessingPool.shutdown();
        networkOperationsPool.shutdown();
        scheduledTasksPool.shutdown();

        try {
            if (!audioProcessingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                audioProcessingPool.shutdownNow();
            }
            if (!networkOperationsPool.awaitTermination(5, TimeUnit.SECONDS)) {
                networkOperationsPool.shutdownNow();
            }
            if (!scheduledTasksPool.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledTasksPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warning("Interrupted during shutdown");
            audioProcessingPool.shutdownNow();
            networkOperationsPool.shutdownNow();
            scheduledTasksPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get audio processing pool for direct access
     */
    public ExecutorService getAudioProcessingPool() {
        return audioProcessingPool;
    }

    /**
     * Get network operations pool for direct access
     */
    public ExecutorService getNetworkOperationsPool() {
        return networkOperationsPool;
    }
}
