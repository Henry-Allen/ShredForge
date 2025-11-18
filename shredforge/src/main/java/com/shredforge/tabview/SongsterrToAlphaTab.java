package com.shredforge.tabview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public final class SongsterrToAlphaTab {
    private static final ObjectMapper M = new ObjectMapper();

    private SongsterrToAlphaTab() {}

    public static String convert(String songsterrJson) {
        try {
            JsonNode src = M.readTree(songsterrJson == null ? "{}" : songsterrJson);
            ObjectNode dst = M.createObjectNode();
            dst.put("version", 1);
            double tempo = src.path("tempo").asDouble(120.0);
            dst.put("tempo", tempo);
            ArrayNode measuresRoot = asArray(src.get("measures"));
            int tsNum = 4;
            int tsDen = 4;
            if (measuresRoot != null && measuresRoot.size() > 0) {
                JsonNode first = measuresRoot.get(0);
                tsNum = first.path("signature").path(0).asInt(4);
                tsDen = first.path("signature").path(1).asInt(4);
            } else {
                tsNum = src.path("timeSignature").path(0).asInt(4);
                tsDen = src.path("timeSignature").path(1).asInt(4);
            }
            ArrayNode time = dst.putArray("timeSignature");
            time.add(tsNum);
            time.add(tsDen);

            // Minimal stylesheet to satisfy alphaTab expectations (tracks[x].score.stylesheet)
            dst.putObject("stylesheet");

            // Build minimal masterBars to satisfy AlphaTab's expected Score shape
            ArrayNode masterBars = dst.putArray("masterBars");
            if (measuresRoot != null && measuresRoot.size() > 0) {
                for (int i = 0; i < measuresRoot.size(); i++) {
                    JsonNode m = measuresRoot.get(i);
                    ObjectNode mb = masterBars.addObject();
                    int mNum = m.path("signature").path(0).asInt(tsNum);
                    int mDen = m.path("signature").path(1).asInt(tsDen);
                    ArrayNode mbTs = mb.putArray("timeSignature");
                    mbTs.add(mNum);
                    mbTs.add(mDen);
                    ObjectNode rg = mb.putObject("repeatGroup");
                    rg.put("opening", false);
                    rg.put("closing", false);
                    rg.put("repeatCount", 0);
                }
            } else {
                ObjectNode mb = masterBars.addObject();
                ArrayNode mbTs = mb.putArray("timeSignature");
                mbTs.add(tsNum);
                mbTs.add(tsDen);
                ObjectNode rg = mb.putObject("repeatGroup");
                rg.put("opening", false);
                rg.put("closing", false);
                rg.put("repeatCount", 0);
            }

            ArrayNode tracks = dst.putArray("tracks");
            ObjectNode track = tracks.addObject();
            // Songsterr track JSON typically has 'name' for the rendered track name
            String trackName = src.path("name").asText(src.path("title").asText("Track"));
            track.put("name", trackName);
            // Minimal playback info required by AlphaTab
            ObjectNode playbackInfo = track.putObject("playbackInfo");
            ObjectNode primary = playbackInfo.putObject("primaryChannel");
            primary.put("program", src.path("program").asInt(25));
            primary.put("volume", src.path("volume").asDouble(1.0));
            primary.put("balance", src.path("balance").asDouble(0.0));
            primary.put("isPercussion", false);
            ObjectNode secondary = playbackInfo.putObject("secondaryChannel");
            secondary.put("program", src.path("program").asInt(25));
            secondary.put("volume", src.path("volume").asDouble(1.0));
            secondary.put("balance", src.path("balance").asDouble(0.0));
            secondary.put("isPercussion", false);
            // Provide back-reference shape: track.score.stylesheet
            ObjectNode trackScore = track.putObject("score");
            trackScore.putObject("stylesheet");
            ArrayNode staves = track.putArray("staves");
            ObjectNode stave = staves.addObject();
            ArrayNode bars = stave.putArray("bars");

            ArrayNode measures = measuresRoot;
            if (measures != null) {
                for (int i = 0; i < measures.size(); i++) {
                    JsonNode m = measures.get(i);
                    ObjectNode bar = bars.addObject();
                    // Keep per-bar time signature when provided by Songsterr
                    int mTsNum = m.path("signature").path(0).asInt(tsNum);
                    int mTsDen = m.path("signature").path(1).asInt(tsDen);
                    ArrayNode barTs = bar.putArray("timeSignature");
                    barTs.add(mTsNum);
                    barTs.add(mTsDen);
                    ArrayNode voices = bar.putArray("voices");
                    ObjectNode voice = voices.addObject();
                    ArrayNode beats = voice.putArray("beats");
                    ArrayNode srcVoices = asArray(m.get("voices"));
                    if (srcVoices == null || srcVoices.isEmpty()) continue;
                    ArrayNode srcBeats = asArray(srcVoices.get(0).get("beats"));
                    if (srcBeats == null) continue;
                    for (int b = 0; b < srcBeats.size(); b++) {
                        JsonNode sb = srcBeats.get(b);
                        ObjectNode db = beats.addObject();
                        ArrayNode dur = db.putArray("duration");
                        double n = sb.path("duration").path(0).asDouble(1.0);
                        double d = sb.path("duration").path(1).asDouble(1.0);
                        dur.add(n); dur.add(d);
                        if (sb.has("dots")) {
                            db.put("dots", sb.get("dots").asInt());
                        }
                        boolean beatRest = sb.path("rest").asBoolean(false);
                        ArrayNode notes = db.putArray("notes");
                        ArrayNode sNotes = asArray(sb.get("notes"));
                        if (beatRest || sNotes == null) {
                            db.put("rest", true);
                            continue;
                        }
                        for (int ni = 0; ni < sNotes.size(); ni++) {
                            JsonNode sn = sNotes.get(ni);
                            if (sn.path("rest").asBoolean(false)) continue;
                            ObjectNode dn = notes.addObject();
                            int string = normalizeString(sn.path("string").asInt(-1));
                            int fret = sn.path("fret").asInt(-1);
                            dn.put("string", string);
                            dn.put("fret", fret);
                            if (sn.path("hammerOn").asBoolean(false)) dn.put("hammerOn", true);
                            if (sn.path("pullOff").asBoolean(false)) dn.put("pullOff", true);
                            if (sn.path("vibrato").asBoolean(false)) dn.put("vibrato", true);
                            if (sn.path("tie").asBoolean(false)) dn.put("tie", true);
                            if (sn.has("slideTo")) dn.put("slideTo", sn.get("slideTo").asInt());
                            if (sn.has("bendSemitones")) dn.put("bendSemitones", sn.get("bendSemitones").asDouble());
                        }
                    }
                }
            }
            return M.writeValueAsString(dst);
        } catch (IOException e) {
            return "{}";
        }
    }

    private static ArrayNode asArray(JsonNode n) {
        return (n != null && n.isArray()) ? (ArrayNode) n : null;
    }

    private static int normalizeString(int s) {
        // Songsterr uses 0-based string indices (0 = highest). AlphaTab expects 1-based.
        if (s < 0) return 1;
        return s + 1;
    }
}
