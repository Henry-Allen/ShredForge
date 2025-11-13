package com.shredforge.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Object pool for audio buffers to reduce garbage collection pressure.
 * Reuses float arrays for continuous audio processing.
 */
public class AudioBufferPool {
    private static final Logger LOGGER = Logger.getLogger(AudioBufferPool.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final int MAX_POOL_SIZE = 20;

    private final Queue<float[]> bufferPool;
    private final int bufferSize;
    private int totalBuffersCreated;

    public AudioBufferPool() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public AudioBufferPool(int bufferSize) {
        this.bufferSize = bufferSize;
        this.bufferPool = new ConcurrentLinkedQueue<>();
        this.totalBuffersCreated = 0;

        // Pre-allocate some buffers
        for (int i = 0; i < 5; i++) {
            bufferPool.offer(new float[bufferSize]);
            totalBuffersCreated++;
        }

        LOGGER.info("Audio buffer pool initialized with buffer size: " + bufferSize);
    }

    /**
     * Acquire a buffer from the pool
     * @return A reusable float array buffer
     */
    public float[] acquire() {
        float[] buffer = bufferPool.poll();

        if (buffer == null) {
            // Pool is empty, create new buffer
            buffer = new float[bufferSize];
            totalBuffersCreated++;

            if (totalBuffersCreated > MAX_POOL_SIZE) {
                LOGGER.fine("Buffer pool exceeded max size, created " + totalBuffersCreated + " buffers");
            }
        } else {
            // Clear the buffer before reuse
            java.util.Arrays.fill(buffer, 0.0f);
        }

        return buffer;
    }

    /**
     * Release a buffer back to the pool for reuse
     * @param buffer The buffer to return
     */
    public void release(float[] buffer) {
        if (buffer == null || buffer.length != bufferSize) {
            LOGGER.warning("Attempted to release invalid buffer");
            return;
        }

        // Only keep pool at reasonable size
        if (bufferPool.size() < MAX_POOL_SIZE) {
            bufferPool.offer(buffer);
        }
        // If pool is full, let GC handle the buffer
    }

    /**
     * Clear all buffers from the pool
     */
    public void clear() {
        bufferPool.clear();
        LOGGER.info("Buffer pool cleared");
    }

    /**
     * Get current pool size
     */
    public int getPoolSize() {
        return bufferPool.size();
    }

    /**
     * Get total buffers created (for monitoring)
     */
    public int getTotalBuffersCreated() {
        return totalBuffersCreated;
    }

    /**
     * Get buffer size
     */
    public int getBufferSize() {
        return bufferSize;
    }
}
