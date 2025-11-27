package com.shredforge.core.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a downloaded Guitar Pro file for a song.
 * 
 * <p>GP files contain the complete tab data in a standard format that AlphaTab can directly consume.
 */
public record TabData(
        String sourceId, 
        String title,
        String artist,
        Path gpFilePath,
        Instant fetchedAt) {

    /**
     * Creates TabData for a GP file.
     */
    public TabData {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(artist, "artist");
        Objects.requireNonNull(gpFilePath, "gpFilePath");
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
    }

    /**
     * Creates TabData from a GP file path.
     */
    public static TabData fromGpFile(String sourceId, String title, String artist, Path gpFilePath) {
        return new TabData(sourceId, title, artist, gpFilePath, Instant.now());
    }

    /**
     * Display name for the song.
     */
    public String displayName() {
        return title + " — " + artist;
    }
}
