package com.shredforge.tabs.model;

import com.shredforge.core.model.SongRequest;
import com.shredforge.tabs.dao.TabDataDao;
import java.util.List;
import java.util.Objects;

/**
 * Represents a song selection from Songsterr search results.
 * Unlike TabSelection which represents a single track, SongSelection represents
 * the entire song with all its tracks (guitar, bass, drums, etc.).
 * 
 * <p>The GP file download will contain all tracks, so we search and select at
 * the song level rather than the track level.
 */
public record SongSelection(
        int songId,
        String artist,
        String title,
        List<TrackInfo> tracks) {

    public SongSelection {
        Objects.requireNonNull(artist, "artist");
        Objects.requireNonNull(title, "title");
        tracks = tracks == null ? List.of() : List.copyOf(tracks);
    }

    /**
     * Creates a SongSelection from a DAO search result.
     */
    public static SongSelection fromSearchResult(TabDataDao.SongSearchResult result) {
        List<TrackInfo> trackInfos = result.tracks().stream()
                .map(t -> new TrackInfo(t.name(), t.instrument(), t.difficulty()))
                .toList();
        return new SongSelection(result.songId(), result.artist(), result.title(), trackInfos);
    }

    /**
     * Unique identifier for this song.
     */
    public String songKey() {
        return String.valueOf(songId);
    }

    /**
     * Converts to a SongRequest for use with the core domain.
     */
    public SongRequest toSongRequest() {
        String instruments = tracks.isEmpty() ? "Unknown" : 
                tracks.stream()
                        .map(TrackInfo::instrument)
                        .distinct()
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown");
        return new SongRequest(title, artist, instruments, "N/A");
    }

    /**
     * Display label for UI lists.
     */
    public String displayLabel() {
        int trackCount = tracks.size();
        String trackSuffix = trackCount == 1 ? "1 track" : trackCount + " tracks";
        return title + " — " + artist + " (" + trackSuffix + ")";
    }

    /**
     * Information about a single track within the song.
     */
    public record TrackInfo(String name, String instrument, int difficulty) {
        public TrackInfo {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(instrument, "instrument");
        }
    }
}
