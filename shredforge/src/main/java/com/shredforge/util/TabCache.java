package com.shredforge.util;

import com.shredforge.model.Tab;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LRU cache for frequently accessed tabs to improve performance.
 * Reduces disk I/O and tab parsing overhead.
 */
public class TabCache {
    private static final Logger LOGGER = Logger.getLogger(TabCache.class.getName());
    private static final int DEFAULT_CACHE_SIZE = 50;

    private final Map<String, CachedTab> cache;
    private final int maxSize;

    private int hits;
    private int misses;

    /**
     * Create tab cache with default size
     */
    public TabCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Create tab cache with custom size
     * @param maxSize Maximum number of tabs to cache
     */
    public TabCache(int maxSize) {
        this.maxSize = maxSize;
        this.hits = 0;
        this.misses = 0;

        // LRU cache using LinkedHashMap
        this.cache = new LinkedHashMap<String, CachedTab>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedTab> eldest) {
                boolean shouldRemove = size() > TabCache.this.maxSize;
                if (shouldRemove) {
                    LOGGER.fine("Evicting tab from cache: " + eldest.getKey());
                }
                return shouldRemove;
            }
        };

        LOGGER.info("Tab cache initialized with max size: " + maxSize);
    }

    /**
     * Get tab from cache
     * @param tabId Tab identifier
     * @return Cached tab or null if not in cache
     */
    public synchronized Tab get(String tabId) {
        if (tabId == null) {
            return null;
        }

        CachedTab cachedTab = cache.get(tabId);

        if (cachedTab != null && !cachedTab.isExpired()) {
            hits++;
            LOGGER.fine("Cache hit for tab: " + tabId);
            return cachedTab.getTab();
        } else {
            misses++;
            if (cachedTab != null) {
                LOGGER.fine("Cache entry expired for tab: " + tabId);
                cache.remove(tabId);
            } else {
                LOGGER.fine("Cache miss for tab: " + tabId);
            }
            return null;
        }
    }

    /**
     * Put tab in cache
     * @param tab Tab to cache
     */
    public synchronized void put(Tab tab) {
        if (tab == null || tab.getId() == null) {
            return;
        }

        cache.put(tab.getId(), new CachedTab(tab));
        LOGGER.fine("Cached tab: " + tab.getId());
    }

    /**
     * Remove tab from cache
     * @param tabId Tab identifier
     */
    public synchronized void remove(String tabId) {
        if (cache.remove(tabId) != null) {
            LOGGER.fine("Removed tab from cache: " + tabId);
        }
    }

    /**
     * Clear entire cache
     */
    public synchronized void clear() {
        cache.clear();
        LOGGER.info("Cache cleared");
    }

    /**
     * Check if tab is in cache
     */
    public synchronized boolean contains(String tabId) {
        CachedTab cachedTab = cache.get(tabId);
        return cachedTab != null && !cachedTab.isExpired();
    }

    /**
     * Get cache statistics
     */
    public synchronized CacheStats getStats() {
        return new CacheStats(hits, misses, cache.size(), maxSize);
    }

    /**
     * Reset statistics
     */
    public synchronized void resetStats() {
        hits = 0;
        misses = 0;
        LOGGER.info("Cache statistics reset");
    }

    /**
     * Wrapper class for cached tabs with expiration
     */
    private static class CachedTab {
        private final Tab tab;
        private final long cachedTime;
        private static final long EXPIRATION_TIME_MS = 30 * 60 * 1000; // 30 minutes

        public CachedTab(Tab tab) {
            this.tab = tab;
            this.cachedTime = System.currentTimeMillis();
        }

        public Tab getTab() {
            return tab;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - cachedTime > EXPIRATION_TIME_MS;
        }
    }

    /**
     * Cache statistics class
     */
    public static class CacheStats {
        private final int hits;
        private final int misses;
        private final int currentSize;
        private final int maxSize;

        public CacheStats(int hits, int misses, int currentSize, int maxSize) {
            this.hits = hits;
            this.misses = misses;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }

        public int getHits() { return hits; }
        public int getMisses() { return misses; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }

        public double getHitRate() {
            int total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Cache Stats: hits=%d, misses=%d, size=%d/%d, hit_rate=%.2f%%",
                hits, misses, currentSize, maxSize, getHitRate() * 100);
        }
    }
}
