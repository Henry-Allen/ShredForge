package com.shredforge.tabs.model;

import com.shredforge.core.model.SongRequest;
import com.shredforge.tabs.util.TuningFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record TabSelection(
        int songId,
        String trackHash,
        String artist,
        String title,
        String trackName,
        String instrument,
        int difficulty,
        List<Integer> tuning) {

    public TabSelection {
        Objects.requireNonNull(trackHash, "trackHash");
        Objects.requireNonNull(artist, "artist");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(trackName, "trackName");
        Objects.requireNonNull(instrument, "instrument");
        tuning = tuning == null ? List.of() : List.copyOf(tuning);
    }

    public String tabId() {
        return songId + ":" + trackHash;
    }

    public SongRequest toSongRequest() {
        String tuningDisplay = TuningFormatter.toDisplayTuning(tuning);
        String difficultyLabel = difficulty >= 0 ? String.valueOf(difficulty) : "N/A";
        return new SongRequest(title, artist, tuningDisplay, difficultyLabel);
    }

    public boolean matches(SongRequest request) {
        boolean titleMatch = request.title() == null
                || title.equalsIgnoreCase(request.title().trim());
        boolean artistMatch = request.artist() == null
                || artist.equalsIgnoreCase(request.artist().trim());
        return titleMatch && artistMatch;
    }

    public String displayLabel() {
        return title + " — " + artist + " • " + trackName + " (" + instrument + ")";
    }
}
