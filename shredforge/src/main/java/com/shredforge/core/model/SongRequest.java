package com.shredforge.core.model;

import java.util.Objects;

/**
 * Lightweight description of a song the user wants to work with.
 */
public record SongRequest(String title, String artist, String tuningPreference, String difficulty) {

    public SongRequest {
        if (isBlank(title) && isBlank(artist)) {
            throw new IllegalArgumentException("At least a title or artist must be provided.");
        }
    }

    public String displayName() {
        if (!isBlank(title) && !isBlank(artist)) {
            return title + " - " + artist;
        }
        return Objects.requireNonNullElse(title, artist);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
