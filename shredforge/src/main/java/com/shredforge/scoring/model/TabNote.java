package com.shredforge.scoring.model;

/**
 * Note event extracted from a tab JSON plan.
 */
public record TabNote(
        double timestampMillis,
        int stringIndex,
        int fret,
        int midi,
        int measureIndex,
        String label) {

    public String describe() {
        return "Measure " + (measureIndex + 1) + " string " + (stringIndex + 1) + " fret " + fret;
    }
}
