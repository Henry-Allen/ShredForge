package com.shredforge.util;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Utility for executing operations with retry logic and exponential backoff.
 * Useful for network operations and other potentially failing tasks.
 */
public class RetryExecutor {
    private static final Logger LOGGER = Logger.getLogger(RetryExecutor.class.getName());

    private final int maxRetries;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;

    /**
     * Create retry executor with default settings
     * Default: 3 retries, 1000ms initial delay, 2x backoff, 10s max delay
     */
    public RetryExecutor() {
        this(3, 1000, 2.0, 10000);
    }

    /**
     * Create retry executor with custom settings
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay between retries in milliseconds
     * @param backoffMultiplier Multiplier for exponential backoff
     * @param maxDelayMs Maximum delay between retries
     */
    public RetryExecutor(int maxRetries, long initialDelayMs, double backoffMultiplier, long maxDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Execute a task with retry logic
     * @param task The task to execute
     * @param <T> Return type
     * @return Result of the task
     * @throws Exception if all retries fail
     */
    public <T> T execute(Callable<T> task) throws Exception {
        return execute(task, null);
    }

    /**
     * Execute a task with retry logic and operation description
     * @param task The task to execute
     * @param operationName Description of operation (for logging)
     * @param <T> Return type
     * @return Result of the task
     * @throws Exception if all retries fail
     */
    public <T> T execute(Callable<T> task, String operationName) throws Exception {
        Exception lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    String opName = operationName != null ? operationName : "operation";
                    LOGGER.info("Retry attempt " + attempt + " for " + opName);
                }

                return task.call();

            } catch (Exception e) {
                lastException = e;

                if (attempt < maxRetries) {
                    if (isRetriableException(e)) {
                        LOGGER.warning("Attempt " + (attempt + 1) + " failed: " + e.getMessage() +
                                      ". Retrying in " + currentDelay + "ms");

                        try {
                            Thread.sleep(currentDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new Exception("Interrupted during retry", ie);
                        }

                        // Exponential backoff
                        currentDelay = Math.min((long)(currentDelay * backoffMultiplier), maxDelayMs);
                    } else {
                        // Non-retriable exception, fail immediately
                        LOGGER.severe("Non-retriable exception: " + e.getMessage());
                        throw e;
                    }
                } else {
                    LOGGER.severe("All retry attempts exhausted");
                }
            }
        }

        // All retries failed
        throw new Exception("Operation failed after " + maxRetries + " retries", lastException);
    }

    /**
     * Determine if an exception is retriable
     */
    private boolean isRetriableException(Exception e) {
        // Network-related exceptions are retriable
        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.UnknownHostException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.io.IOException) {
            return true;
        }

        // Check exception message for common retriable errors
        String message = e.getMessage();
        if (message != null) {
            message = message.toLowerCase();
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("network") ||
                   message.contains("refused") ||
                   message.contains("unavailable");
        }

        return false;
    }

    /**
     * Execute a runnable task with retry logic
     */
    public void execute(Runnable task, String operationName) throws Exception {
        execute(() -> {
            task.run();
            return null;
        }, operationName);
    }
}
