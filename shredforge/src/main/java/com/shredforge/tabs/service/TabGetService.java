package com.shredforge.tabs.service;

import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.tabs.dao.TabDataDao;
import com.shredforge.tabs.model.TabSearchRequest;
import com.shredforge.tabs.model.TabSelection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TabGetService {

    private static final String SOURCE_PREFIX = "songsterr:";

    private final TabDataDao dao;
    private final ConcurrentMap<String, TabSelection> selectionCache = new ConcurrentHashMap<>();

    public TabGetService(TabDataDao dao) {
        this.dao = dao;
    }

    public List<TabSelection> search(TabSearchRequest request) {
        List<TabSelection> selections = new ArrayList<>();
        for (TabDataDao.SongSearchResult song : dao.searchSongs(request.cleanedTerm())) {
            for (TabDataDao.TrackSummary track : song.tracks()) {
                TabSelection selection = new TabSelection(
                        song.songId(),
                        track.hash(),
                        song.artist(),
                        song.title(),
                        track.name(),
                        track.instrument(),
                        track.difficulty(),
                        track.tuning());
                registerSelection(selection);
                selections.add(selection);
            }
        }
        return selections;
    }

    public TabData download(TabSelection selection) {
        registerSelection(selection);
        TabDataDao.SongDetails details = dao.fetchSongDetails(selection);
        ResolvedTrack resolved = resolveTrack(selection, details);
        String raw = dao.fetchTabJson(details.revisionId(), resolved.meta.partId());
        return new TabData(
                buildSourceId(resolved.selection), resolved.selection.toSongRequest(), raw, Instant.now(), null);
    }

    public Optional<TabData> downloadBySongRequest(SongRequest request) {
        String term = request.title() != null ? request.title() : request.artist();
        if (term == null || term.isBlank()) {
            return Optional.empty();
        }
        List<TabSelection> selections = search(new TabSearchRequest(term));
        return selections.stream()
                .filter(selection -> selection.matches(request))
                .findFirst()
                .map(this::download);
    }

    public Optional<TabSelection> findSelection(String tabId) {
        return Optional.ofNullable(selectionCache.get(tabId));
    }

    public void registerSelection(TabSelection selection) {
        selectionCache.put(selection.tabId(), selection);
    }

    public static String buildSourceId(TabSelection selection) {
        return SOURCE_PREFIX + selection.tabId();
    }

    public static String extractTabId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Tab data is missing a source identifier.");
        }
        return sourceId.startsWith(SOURCE_PREFIX)
                ? sourceId.substring(SOURCE_PREFIX.length())
                : sourceId;
    }

    private ResolvedTrack resolveTrack(TabSelection selection, TabDataDao.SongDetails details) {
        TabDataDao.TrackMeta meta = details.track(selection.trackHash());
        if (meta != null) {
            return new ResolvedTrack(selection, meta);
        }
        List<TabDataDao.TrackSummary> fallbacks = dao.searchSongs(selection.title())
                .stream()
                .filter(song -> song.songId() == selection.songId())
                .findFirst()
                .map(TabDataDao.SongSearchResult::tracks)
                .orElse(List.of());
        for (TabDataDao.TrackSummary summary : fallbacks) {
            TabDataDao.TrackMeta fallbackMeta = details.track(summary.hash());
            if (fallbackMeta != null) {
                TabSelection updated = new TabSelection(
                        selection.songId(),
                        summary.hash(),
                        selection.artist(),
                        selection.title(),
                        summary.name(),
                        summary.instrument(),
                        summary.difficulty(),
                        summary.tuning());
                registerSelection(updated);
                return new ResolvedTrack(updated, fallbackMeta);
            }
        }
        throw new IllegalStateException("Songsterr did not return metadata for the requested track.");
    }

    private record ResolvedTrack(TabSelection selection, TabDataDao.TrackMeta meta) {}
}
