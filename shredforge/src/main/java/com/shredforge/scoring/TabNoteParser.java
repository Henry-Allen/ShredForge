package com.shredforge.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.shredforge.core.model.TabData;
import com.shredforge.scoring.model.DetectedNote;
import com.shredforge.scoring.model.TabNote;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts Songsterr tab JSON into a simplified list of timed note events.
 */
public final class TabNoteParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<Integer> STANDARD_TUNING = List.of(40, 45, 50, 55, 59, 64); // E2..E4

    public List<TabNote> parse(TabData tabData) {
        try {
            JsonNode root = MAPPER.readTree(tabData.rawContent());
            ArrayNode measures = asArray(root.get("measures"));
            if (measures == null || measures.isEmpty()) {
                return List.of();
            }
            List<Integer> tuning = readTuning(root);
            double tempo = root.path("tempo").asDouble(120.0);
            double quarterNoteMillis = 60000.0 / Math.max(tempo, 1.0);

            List<TabNote> notes = new ArrayList<>();
            double cursorMs = 0.0;
            for (int i = 0; i < measures.size(); i++) {
                JsonNode measure = measures.get(i);
                ArrayNode voices = asArray(measure.get("voices"));
                if (voices == null || voices.isEmpty()) {
                    cursorMs += quarterNoteMillis * 4;
                    continue;
                }
                JsonNode primaryVoice = voices.get(0);
                ArrayNode beats = asArray(primaryVoice.get("beats"));
                if (beats == null || beats.isEmpty()) {
                    cursorMs += quarterNoteMillis * 4;
                    continue;
                }
                for (JsonNode beat : beats) {
                    double beatUnits = durationToUnits(beat);
                    double beatLengthMs = beatUnits * quarterNoteMillis;
                    ArrayNode beatNotes = asArray(beat.get("notes"));
                    if (beatNotes != null) {
                        for (JsonNode note : beatNotes) {
                            if (note.path("rest").asBoolean(false)) {
                                continue;
                            }
                            int stringIndex = normalizeStringIndex(note.path("string").asInt(-1));
                            if (stringIndex < 0 || stringIndex >= tuning.size()) {
                                continue;
                            }
                            int fret = note.path("fret").asInt(-1);
                            if (fret < 0) {
                                continue;
                            }
                            int midi = tuning.get(stringIndex) + fret;
                            notes.add(new TabNote(
                                    cursorMs,
                                    stringIndex,
                                    fret,
                                    midi,
                                    i,
                                    buildLabel(stringIndex, fret, midi)));
                        }
                    }
                    cursorMs += beatLengthMs;
                }
            }
            return notes;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static ArrayNode asArray(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : null;
    }

    private static double durationToUnits(JsonNode beat) {
        ArrayNode duration = asArray(beat.get("duration"));
        if (duration == null || duration.size() < 2) {
            return 1.0;
        }
        double numerator = duration.get(0).asDouble(1.0);
        double denominator = duration.get(1).asDouble(1.0);
        if (denominator == 0) {
            return 1.0;
        }
        return numerator / denominator;
    }

    private static List<Integer> readTuning(JsonNode root) {
        ArrayNode tuningNode = asArray(root.get("tuning"));
        if (tuningNode == null || tuningNode.isEmpty()) {
            return STANDARD_TUNING;
        }
        List<Integer> tuning = new ArrayList<>();
        tuningNode.forEach(node -> tuning.add(node.asInt()));
        return tuning.isEmpty() ? STANDARD_TUNING : tuning;
    }

    private static int normalizeStringIndex(int raw) {
        if (raw < 0) {
            return -1;
        }
        if (raw == 0) {
            return 0;
        }
        return raw - 1; // Songsterr indexes strings starting at 1
    }

    private static String buildLabel(int stringIndex, int fret, int midi) {
        return "S" + (stringIndex + 1) + " F" + fret + " (" + DetectedNote.midiToNoteName(midi) + ")";
    }
}
