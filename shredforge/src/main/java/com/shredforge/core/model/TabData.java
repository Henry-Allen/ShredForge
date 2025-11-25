package com.shredforge.core.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Raw tablature data as returned by an external source.
 * 
 * <p>Supports two content types:
 * <ul>
 *   <li><b>JSON content</b>: Songsterr's internal JSON format (stored in {@code rawContent})</li>
 *   <li><b>GP file</b>: Guitar Pro binary file (stored at {@code gpFilePath})</li>
 * </ul>
 * 
 * <p>When a GP file is available, it should be preferred for rendering as it contains
 * the complete tab data in a standard format that AlphaTab can directly consume.
 */
public record TabData(
        String sourceId, 
        SongRequest song, 
        String rawContent, 
        Instant fetchedAt, 
        Path cachedLocation,
        Path gpFilePath) {

    /**
     * Creates TabData with all fields.
     */
    public TabData {
        Objects.requireNonNull(song, "song");
        // rawContent can be null if we only have a GP file
        if (rawContent == null && gpFilePath == null) {
            throw new IllegalArgumentException("Either rawContent or gpFilePath must be provided");
        }
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
    }

    /**
     * Backwards-compatible constructor without GP file path.
     */
    public TabData(String sourceId, SongRequest song, String rawContent, Instant fetchedAt, Path cachedLocation) {
        this(sourceId, song, rawContent, fetchedAt, cachedLocation, null);
    }

    /**
     * Creates TabData with only a GP file (no JSON content).
     */
    public static TabData fromGpFile(String sourceId, SongRequest song, Path gpFilePath, Instant fetchedAt) {
        return new TabData(sourceId, song, null, fetchedAt, null, gpFilePath);
    }

    public boolean isCachedLocally() {
        return cachedLocation != null;
    }

    /**
     * Returns true if this tab has a Guitar Pro file available.
     */
    public boolean hasGpFile() {
        return gpFilePath != null;
    }

    /**
     * Returns true if this tab has JSON content available.
     */
    public boolean hasJsonContent() {
        return rawContent != null && !rawContent.isBlank();
    }
}
