
package com.shredforge.core.model;

import java.util.Objects;

/**
 * Represents a note expected from the tab/score at a specific time.
 * Extracted from AlphaTab's loaded score and used for scoring comparisons.
 */
public record ExpectedNote(
        double timeMs,
        double durationMs,
        int midi,
        int string,
        int fret,
        int measureIndex,
        int beatIndex,
        String noteName) {

    public ExpectedNote {
        Objects.requireNonNull(noteName, "noteName");
        // Allow negative timeMs (can happen with pickup measures) - clamp to 0
        if (timeMs < 0) {
            timeMs = 0;
        }
        if (durationMs < 0) {
            durationMs = 0;
        }
    }

    /**
     * Creates an ExpectedNote with auto-generated note name.
     */
    public static ExpectedNote of(double timeMs, double durationMs, int midi, int string, int fret,
                                   int measureIndex, int beatIndex) {
        return new ExpectedNote(timeMs, durationMs, midi, string, fret, measureIndex, beatIndex, midiToNoteName(midi));
    }

    /**
     * Converts MIDI note number to note name (e.g., 60 -> "C4").
     */
    public static String midiToNoteName(int midi) {
        if (midi < 0 || midi > 127) {
            return "--";
        }
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int noteIndex = midi % 12;
        int octave = (midi / 12) - 1;
        return names[noteIndex] + octave;
    }

    /**
     * Returns the frequency in Hz for this note's MIDI value.
     */
    public double frequencyHz() {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    /**
     * Checks if the given time falls within this note's active window.
     */
    public boolean isActiveAt(double timeMs, double toleranceMs) {
        return timeMs >= (this.timeMs - toleranceMs) && timeMs <= (this.timeMs + durationMs + toleranceMs);
    }
}
