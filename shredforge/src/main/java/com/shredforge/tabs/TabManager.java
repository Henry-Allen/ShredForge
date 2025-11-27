package com.shredforge.tabs;

import com.shredforge.core.model.TabData;
import com.shredforge.tabs.dao.TabDataDao;
import com.shredforge.tabs.model.SongSelection;
import com.shredforge.tabs.model.TabSearchRequest;
import com.shredforge.tabs.service.TabGetService;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * High-level manager that coordinates song search and GP file downloads.
 * 
 * <p>Songs are searched at the song level (not individual tracks), and downloads
 * return GP files that contain all tracks for the song. Downloads use direct HTTP
 * to Songsterr's API rather than browser automation.
 */
public final class TabManager {

    private final TabDataDao dao;
    private final TabGetService getService;

    public TabManager(TabDataDao dao, TabGetService getService) {
        this.dao = dao;
        this.getService = getService;
    }

    public static TabManager createDefault() {
        TabDataDao dao = new TabDataDao();
        return new TabManager(dao, new TabGetService(dao));
    }

    /**
     * Searches for songs matching the given request.
     * Returns song-level results (each containing all tracks).
     */
    public List<SongSelection> searchSongs(TabSearchRequest request) {
        return getService.searchSongs(request);
    }

    /**
     * Downloads a Guitar Pro file for the given song.
     * The GP file contains all tracks for the song.
     * 
     * @param selection the song to download
     * @return a CompletableFuture that resolves to TabData with the GP file path
     */
    public CompletableFuture<TabData> downloadGpFile(SongSelection selection) {
        return getService.downloadGpFile(selection);
    }

    /**
     * Checks if a GP file already exists for the given song.
     * If it exists, returns TabData pointing to the cached file.
     */
    public Optional<TabData> findCachedGpFile(SongSelection selection) {
        return getService.findExistingGpFile(selection)
                .map(path -> TabData.fromGpFile(
                        TabGetService.buildSourceId(selection),
                        selection.title(),
                        selection.artist(),
                        path
                ));
    }

    /**
     * Downloads a GP file, or returns the cached version if it exists.
     */
    public CompletableFuture<TabData> downloadOrGetCached(SongSelection selection) {
        return findCachedGpFile(selection)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> downloadGpFile(selection));
    }

    public Optional<SongSelection> findSelection(String songKey) {
        return getService.findSelection(songKey);
    }

    /**
     * Returns the GP storage directory.
     */
    public Path getGpStorageDir() {
        return dao.getGpStorageDir();
    }
}
