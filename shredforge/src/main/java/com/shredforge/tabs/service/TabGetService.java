package com.shredforge.tabs.service;

import com.shredforge.core.model.TabData;
import com.shredforge.tabs.dao.TabDataDao;
import com.shredforge.tabs.model.SongSelection;
import com.shredforge.tabs.model.TabSearchRequest;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for searching and downloading songs from Songsterr.
 * Downloads are GP files (containing all tracks) rather than individual track JSON.
 */
public final class TabGetService {

    private static final String SOURCE_PREFIX = "songsterr:";

    private final TabDataDao dao;
    private final ConcurrentMap<String, SongSelection> selectionCache = new ConcurrentHashMap<>();

    public TabGetService(TabDataDao dao) {
        this.dao = dao;
    }

    /**
     * Searches for songs matching the given request.
     * Returns song-level results (each containing all tracks).
     */
    public List<SongSelection> searchSongs(TabSearchRequest request) {
        return dao.searchSongs(request.cleanedTerm()).stream()
                .map(SongSelection::fromSearchResult)
                .peek(this::registerSelection)
                .toList();
    }

    /**
     * Downloads the GP file for the given song selection.
     * The GP file contains all tracks for the song.
     * 
     * @param selection the song to download
     * @return a CompletableFuture that resolves to TabData with the GP file path
     */
    public CompletableFuture<TabData> downloadGpFile(SongSelection selection) {
        registerSelection(selection);
        
        return dao.downloadGpFile(selection)
            .thenApply(gpPath -> TabData.fromGpFile(
                buildSourceId(selection),
                selection.toSongRequest(),
                gpPath,
                Instant.now()
            ));
    }

    /**
     * Checks if a GP file already exists for the given song.
     */
    public Optional<Path> findExistingGpFile(SongSelection selection) {
        Path expectedPath = dao.getGpStorageDir().resolve(
            sanitize(selection.songId() + "_" + selection.artist() + "_" + selection.title()) + ".gp");
        return expectedPath.toFile().exists() ? Optional.of(expectedPath) : Optional.empty();
    }

    public Optional<SongSelection> findSelection(String songKey) {
        return Optional.ofNullable(selectionCache.get(songKey));
    }

    public void registerSelection(SongSelection selection) {
        selectionCache.put(selection.songKey(), selection);
    }

    public static String buildSourceId(SongSelection selection) {
        return SOURCE_PREFIX + selection.songKey();
    }

    public static String extractSongKey(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Tab data is missing a source identifier.");
        }
        return sourceId.startsWith(SOURCE_PREFIX)
                ? sourceId.substring(SOURCE_PREFIX.length())
                : sourceId;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "_").replaceAll("_+", "_");
    }
}
