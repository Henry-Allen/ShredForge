package com.shredforge.scoring.model;

import java.util.Collections;
import java.util.List;

public record ScoreReport(List<NoteMatch> matches, int totalNotes, int hits, int misses, double durationMillis, List<String> warnings) {

    public ScoreReport {
        matches = matches == null ? List.of() : List.copyOf(matches);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public double accuracyPercent() {
        if (totalNotes == 0) {
            return 0.0;
        }
        return (hits * 100.0) / totalNotes;
    }

    public List<NoteMatch> missedNotes() {
        return matches.stream().filter(match -> !match.hit()).toList();
    }

    public static ScoreReport empty(String message) {
        return new ScoreReport(Collections.emptyList(), 0, 0, 0, 0, List.of(message));
    }
}
