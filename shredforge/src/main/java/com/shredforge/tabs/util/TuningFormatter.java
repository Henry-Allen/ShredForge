package com.shredforge.tabs.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Helper utilities for formatting Songsterr metadata consistently.
 */
public final class TuningFormatter {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private TuningFormatter() {}

    public static String toDisplayTuning(List<Integer> tuning) {
        if (tuning == null || tuning.isEmpty()) {
            return "Unknown";
        }
        return tuning.stream()
                .map(TuningFormatter::midiToNote)
                .collect(Collectors.joining(" "));
    }

    public static String midiToNote(int midi) {
        int note = (midi % 12 + 12) % 12;
        int octave = (midi / 12) - 1;
        return NOTE_NAMES[note] + octave;
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "song";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-");
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.isEmpty() ? "song" : slug;
    }
}
