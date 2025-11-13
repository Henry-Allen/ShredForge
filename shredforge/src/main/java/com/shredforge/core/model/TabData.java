package com.shredforge.core.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Raw tablature data as returned by an external source.
 */
public record TabData(String sourceId, SongRequest song, String rawContent, Instant fetchedAt, Path cachedLocation) {

    public TabData {
        Objects.requireNonNull(song, "song");
        Objects.requireNonNull(rawContent, "rawContent");
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
    }

    public boolean isCachedLocally() {
        return cachedLocation != null;
    }
}
