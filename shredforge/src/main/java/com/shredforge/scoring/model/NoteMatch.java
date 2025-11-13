package com.shredforge.scoring.model;

public record NoteMatch(TabNote expected, DetectedNote actual, double timeDeltaMillis, double centsDelta, boolean hit) {

    public static NoteMatch miss(TabNote expected) {
        return new NoteMatch(expected, null, Double.NaN, Double.NaN, false);
    }
}
