package com.shredforge.tabview.render;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SongsterrTabFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double LEFT_MARGIN = 56.0;
    private static final double RIGHT_MARGIN = 32.0;
    private static final double TOP_MARGIN = 32.0;
    private static final double BOTTOM_MARGIN = 48.0;
    private static final double STRING_SPACING = 22.0;
    private static final double UNIT_WIDTH = 240.0;
    private static final double MIN_BEAT_WIDTH = 34.0;
    private static final double MIN_MEASURE_WIDTH = 460.0;
    private static final double NOTE_FONT_SIZE = 15.0;
    private static final double GRACE_FONT_SIZE = 11.0;
    private static final Locale LOCALE = Locale.US;

    private SongsterrTabFormatter() {
    }

    public static String renderMessage(String message) {
        return wrapHtml("<div class=\"empty-state\">" + escape(message) + "</div>");
    }

    public static String render(String rawJson, List<Integer> trackTuning, String instrumentName) {
        if (rawJson == null || rawJson.isBlank()) {
            return renderMessage("Tab data is not available yet.");
        }
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            if (isDrumPart(root, instrumentName)) {
                return renderMessage("Drum tracks are not supported.");
            }
            ArrayNode measures = asArray(root.get("measures"));
            if (measures == null || measures.isEmpty()) {
                return renderMessage("No measures found in tab JSON.");
            }
            List<String> stringLabels = buildStringLabels(root, trackTuning);
            if (stringLabels.isEmpty()) {
                return renderMessage("Unable to determine string tuning for this track.");
            }
            boolean zeroBasedStrings = detectZeroBasedNumbering(measures, stringLabels.size());
            SvgDocument document = new SvgDocument(stringLabels, zeroBasedStrings);
            return document.render(measures);
        } catch (Exception ex) {
            return renderMessage("Failed to render tab: " + escape(ex.getMessage()));
        }
    }

    private static boolean isDrumPart(JsonNode root, String instrumentName) {
        int instrumentId = root.path("instrumentId").asInt(-1);
        if (instrumentId == 1024) {
            return true;
        }
        if (instrumentName == null) {
            return false;
        }
        String lower = instrumentName.toLowerCase(Locale.ROOT);
        return lower.contains("drum");
    }

    private static List<String> buildStringLabels(JsonNode root, List<Integer> trackTuning) {
        List<Integer> tuning = extractTuning(root, trackTuning);
        if (tuning.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>(tuning.size());
        for (int i = 0; i < tuning.size(); i++) {
            int midi = tuning.get(i);
            String note = midiToNoteName(midi);
            if (i == 0 && "E".equalsIgnoreCase(note)) {
                labels.add("e");
            } else {
                labels.add(note.toUpperCase(Locale.ROOT));
            }
        }
        return labels;
    }

    private static List<Integer> extractTuning(JsonNode root, List<Integer> trackTuning) {
        ArrayNode tuningNode = asArray(root.get("tuning"));
        List<Integer> tuning = new ArrayList<>();
        if (tuningNode != null) {
            tuningNode.forEach(node -> tuning.add(node.asInt()));
        } else if (trackTuning != null) {
            tuning.addAll(trackTuning);
        }
        if (tuning.isEmpty()) {
            int strings = Math.max(6, root.path("strings").asInt(6));
            for (int i = 0; i < strings; i++) {
                tuning.add(0);
            }
        }
        return tuning;
    }

    private static boolean detectZeroBasedNumbering(ArrayNode measures, int stringCount) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (JsonNode measure : measures) {
            ArrayNode voices = asArray(measure.get("voices"));
            if (voices == null) continue;
            for (JsonNode voice : voices) {
                ArrayNode beats = asArray(voice.get("beats"));
                if (beats == null) continue;
                for (JsonNode beat : beats) {
                    ArrayNode notes = asArray(beat.get("notes"));
                    if (notes == null) continue;
                    for (JsonNode note : notes) {
                        if (note.path("rest").asBoolean(false)) continue;
                        if (!note.has("string")) continue;
                        int stringValue = note.get("string").asInt(-1);
                        if (stringValue < 0) continue;
                        min = Math.min(min, stringValue);
                        max = Math.max(max, stringValue);
                    }
                }
            }
        }
        if (min == Integer.MAX_VALUE) {
            return true;
        }
        if (min >= 1 && max <= stringCount) {
            return false;
        }
        return true;
    }

    private static ArrayNode asArray(JsonNode node) {
        return node != null && node.isArray() ? (ArrayNode) node : null;
    }

    private static String midiToNoteName(int midi) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int index = Math.floorMod(midi, names.length);
        return names[index];
    }

    private static double durationToUnits(JsonNode beat) {
        ArrayNode duration = asArray(beat.get("duration"));
        if (duration == null || duration.size() < 2) {
            return 1.0;
        }
        double numerator = duration.get(0).asDouble(1.0);
        double denominator = duration.get(1).asDouble(1.0);
        if (denominator == 0.0) {
            return 1.0;
        }
        return numerator / denominator;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public static String wrapHtml(String bodyContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n")
                .append("<head>\n")
                .append("  <meta charset=\"UTF-8\"/>\n")
                .append("  <style>\n")
                .append("    :root { color-scheme: dark; }\n")
                .append("    body {\n")
                .append("      margin: 0;\n")
                .append("      padding: 0;\n")
                .append("      font-family: \"Inter\", \"Segoe UI\", sans-serif;\n")
                .append("      background-color: #0d1118;\n")
                .append("      color: #e8eef8;\n")
                .append("    }\n")
                .append("    .tab-container {\n")
                .append("      display: flex;\n")
                .append("      flex-direction: column;\n")
                .append("      gap: 28px;\n")
                .append("      padding: 18px;\n")
                .append("    }\n")
                .append("    .measure {\n")
                .append("      display: flex;\n")
                .append("      flex-direction: column;\n")
                .append("      gap: 8px;\n")
                .append("    }\n")
                .append("    .measure-marker {\n")
                .append("      font-size: 13px;\n")
                .append("      font-weight: 600;\n")
                .append("      letter-spacing: 0.02em;\n")
                .append("      color: #ffca6f;\n")
                .append("      text-transform: uppercase;\n")
                .append("    }\n")
                .append("    .tab-svg {\n")
                .append("      border-radius: 14px;\n")
                .append("      background: linear-gradient(135deg, rgba(25,31,40,0.94), rgba(17,21,29,0.98));\n")
                .append("      box-shadow: 0 22px 40px rgba(7, 9, 14, 0.55), inset 0 0 0 1px rgba(255,255,255,0.03);\n")
                .append("    }\n")
                .append("    .string-line {\n")
                .append("      stroke: rgba(194, 208, 228, 0.68);\n")
                .append("      stroke-width: 1.2;\n")
                .append("    }\n")
                .append("    .measure-bar {\n")
                .append("      stroke: rgba(194, 208, 228, 0.84);\n")
                .append("      stroke-width: 1.6;\n")
                .append("    }\n")
                .append("    .repeat-line {\n")
                .append("      stroke: rgba(224, 232, 247, 0.9);\n")
                .append("      stroke-width: 2.2;\n")
                .append("    }\n")
                .append("    .repeat-line.thin { stroke-width: 1.2; }\n")
                .append("    .repeat-dot {\n")
                .append("      fill: rgba(224, 232, 247, 0.88);\n")
                .append("    }\n")
                .append("    .string-label {\n")
                .append("      fill: rgba(168, 181, 204, 0.85);\n")
                .append("      font-size: 12px;\n")
                .append("      font-weight: 500;\n")
                .append("      text-anchor: end;\n")
                .append("      dominant-baseline: middle;\n")
                .append("    }\n")
                .append("    .note {\n")
                .append("      fill: #f4f7fb;\n")
                .append("      font-family: \"SFMono-Regular\", \"Menlo\", \"Consolas\", monospace;\n")
                .append("      font-weight: 600;\n")
                .append("      font-size: ").append(String.format(LOCALE, "%.1fpx", NOTE_FONT_SIZE)).append(";\n")
                .append("      dominant-baseline: middle;\n")
                .append("      text-anchor: middle;\n")
                .append("    }\n")
                .append("    .note.grace {\n")
                .append("      font-size: ").append(String.format(LOCALE, "%.1fpx", GRACE_FONT_SIZE)).append(";\n")
                .append("      opacity: 0.78;\n")
                .append("    }\n")
                .append("    .note.ghost {\n")
                .append("      opacity: 0.55;\n")
                .append("      font-style: italic;\n")
                .append("    }\n")
                .append("    .note.dead {\n")
                .append("      fill: #ffb0b0;\n")
                .append("      font-weight: 700;\n")
                .append("    }\n")
                .append("    .effect-label {\n")
                .append("      fill: rgba(240, 242, 255, 0.85);\n")
                .append("      font-size: 11px;\n")
                .append("      font-weight: 600;\n")
                .append("      text-anchor: middle;\n")
                .append("    }\n")
                .append("    .effect-label.muted { fill: #7fc4ff; }\n")
                .append("    .effect-label.let-ring { fill: #9ff2be; }\n")
                .append("    .effect-label.hp { fill: #fbd38d; }\n")
                .append("    .effect-label.dynamic { fill: #f7b469; font-size: 10.5px; }\n")
                .append("    .effect-label.text { fill: rgba(227, 234, 255, 0.95); font-size: 10.5px; }\n")
                .append("    .effect-label.accidental { fill: rgba(255, 209, 156, 0.92); }\n")
                .append("    .vibrato-wave {\n")
                .append("      stroke: rgba(126, 188, 255, 0.92);\n")
                .append("      stroke-width: 1.6;\n")
                .append("      fill: none;\n")
                .append("    }\n")
                .append("    .vibrato-wave.wide { stroke-width: 1.9; }\n")
                .append("    .vibrato-wave.whammy { stroke-dasharray: 4 2; }\n")
                .append("    .connection.slide {\n")
                .append("      stroke: rgba(111, 178, 255, 0.95);\n")
                .append("      stroke-width: 1.8;\n")
                .append("      fill: none;\n")
                .append("      stroke-linecap: round;\n")
                .append("    }\n")
                .append("    .connection.slide.legato { stroke-dasharray: 6 4; stroke-width: 1.6; }\n")
                .append("    .connection.tie {\n")
                .append("      stroke: rgba(255, 210, 120, 0.92);\n")
                .append("      stroke-width: 1.5;\n")
                .append("      fill: none;\n")
                .append("    }\n")
                .append("    .connection.hammer, .connection.pull {\n")
                .append("      stroke: rgba(250, 197, 120, 0.9);\n")
                .append("      stroke-width: 1.4;\n")
                .append("      fill: none;\n")
                .append("    }\n")
                .append("    .connection.pull { stroke-dasharray: 4 3; }\n")
                .append("    .stroke-arrow {\n")
                .append("      stroke: rgba(184, 216, 255, 0.9);\n")
                .append("      stroke-width: 1.8;\n")
                .append("      fill: none;\n")
                .append("      stroke-linecap: round;\n")
                .append("    }\n")
                .append("    .stroke-arrow.down { stroke: rgba(255, 193, 134, 0.92); }\n")
                .append("    .stroke-arrow-head {\n")
                .append("      fill: rgba(184, 216, 255, 0.9);\n")
                .append("    }\n")
                .append("    .stroke-arrow-head.down { fill: rgba(255, 193, 134, 0.92); }\n")
                .append("    .rest-shape {\n")
                .append("      stroke: rgba(208, 216, 230, 0.9);\n")
                .append("      stroke-width: 1.6;\n")
                .append("      fill: none;\n")
                .append("      stroke-linecap: round;\n")
                .append("    }\n")
                .append("    .rest-rect {\n")
                .append("      fill: rgba(208, 216, 230, 0.9);\n")
                .append("    }\n")
                .append("    .staccato-dot {\n")
                .append("      fill: rgba(237, 240, 255, 0.9);\n")
                .append("    }\n")
                .append("    .measure-signature {\n")
                .append("      fill: rgba(212, 222, 240, 0.85);\n")
                .append("      font-size: 13px;\n")
                .append("      font-weight: 600;\n")
                .append("    }\n")
                .append("    .repeat-marker {\n")
                .append("      fill: rgba(218, 228, 246, 0.88);\n")
                .append("      font-size: 11px;\n")
                .append("      font-weight: 600;\n")
                .append("    }\n")
                .append("    .tuplet-bracket {\n")
                .append("      stroke: rgba(191, 201, 225, 0.85);\n")
                .append("      stroke-width: 1.2;\n")
                .append("      fill: none;\n")
                .append("    }\n")
                .append("    .tuplet-label {\n")
                .append("      fill: rgba(191, 201, 225, 0.9);\n")
                .append("      font-size: 11px;\n")
                .append("      font-weight: 600;\n")
                .append("      text-anchor: middle;\n")
                .append("    }\n")
                .append("    .bend-path {\n")
                .append("      stroke: rgba(255, 196, 138, 0.95);\n")
                .append("      stroke-width: 1.6;\n")
                .append("      fill: none;\n")
                .append("      stroke-linecap: round;\n")
                .append("    }\n")
                .append("    .bend-path.release { stroke-dasharray: 5 3; }\n")
                .append("    .bend-text {\n")
                .append("      fill: rgba(255, 212, 154, 0.95);\n")
                .append("      font-size: 10px;\n")
                .append("      font-weight: 600;\n")
                .append("      text-anchor: middle;\n")
                .append("    }\n")
                .append("    .bend-label {\n")
                .append("      fill: rgba(255, 212, 154, 0.95);\n")
                .append("      font-size: 11px;\n")
                .append("      font-weight: 600;\n")
                .append("      text-anchor: start;\n")
                .append("    }\n")
                .append("    .empty-state {\n")
                .append("      min-height: 220px;\n")
                .append("      display: flex;\n")
                .append("      align-items: center;\n")
                .append("      justify-content: center;\n")
                .append("      font-size: 16px;\n")
                .append("      letter-spacing: 0.02em;\n")
                .append("      padding: 24px;\n")
                .append("      color: rgba(224, 232, 242, 0.82);\n")
                .append("    }\n")
                .append("  </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("  <div class=\"tab-container\">\n");
        sb.append(bodyContent);
        sb.append("  </div>\n")
                .append("</body>\n")
                .append("</html>\n");
        return sb.toString();
    }

    public static List<String> extractMeasureFragments(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<String> rawMeasures = sliceMeasures(html);
        if (rawMeasures.isEmpty()) {
            return List.of(html);
        }
        List<String> wrapped = new ArrayList<>(rawMeasures.size());
        for (String measure : rawMeasures) {
            wrapped.add(wrapHtml("<div class=\"tab-container\">" + measure + "</div>"));
        }
        return wrapped;
    }

    private static List<String> sliceMeasures(String html) {
        List<String> measures = new ArrayList<>();
        String marker = "<div class=\"measure\">";
        int index = html.indexOf(marker);
        while (index != -1) {
            int end = findMatchingDiv(html, index + marker.length());
            if (end == -1) {
                break;
            }
            measures.add(html.substring(index, end));
            index = html.indexOf(marker, end);
        }
        return measures;
    }

    private static int findMatchingDiv(String html, int start) {
        int depth = 1;
        int position = start;
        while (position < html.length()) {
            int nextOpen = html.indexOf("<div", position);
            int nextClose = html.indexOf("</div>", position);
            if (nextClose == -1) {
                return -1;
            }
            if (nextOpen != -1 && nextOpen < nextClose) {
                depth++;
                position = nextOpen + 4;
                continue;
            }
            depth--;
            position = nextClose + 6;
            if (depth == 0) {
                return position;
            }
        }
        return -1;
    }

    private static final class SvgDocument {
        private final List<String> labels;
        private final boolean zeroBasedStrings;
        private final Map<Integer, NotePosition> lastNotePerString = new HashMap<>();
        private final StringBuilder html = new StringBuilder();

        SvgDocument(List<String> labels, boolean zeroBasedStrings) {
            this.labels = labels;
            this.zeroBasedStrings = zeroBasedStrings;
        }

        String render(ArrayNode measures) {
            for (int i = 0; i < measures.size(); i++) {
                appendMeasure(measures.get(i), i + 1);
            }
            return wrapHtml(html.toString());
        }

        private void appendMeasure(JsonNode measure, int measureIndex) {
            ArrayNode voices = asArray(measure.get("voices"));
            if (voices == null || voices.isEmpty()) {
                html.append(renderEmptyMeasure(measure, measureIndex));
                return;
            }
            JsonNode primaryVoice = voices.get(0);
            ArrayNode beats = asArray(primaryVoice.get("beats"));
            if (beats == null || beats.isEmpty()) {
                html.append(renderEmptyMeasure(measure, measureIndex));
                return;
            }

            List<BeatInfo> infos = new ArrayList<>();
            double totalUnits = 0.0;
            for (JsonNode beat : beats) {
                double units = Math.max(0.125, durationToUnits(beat));
                BeatInfo info = new BeatInfo(beat, units);
                infos.add(info);
                totalUnits += units;
            }

            double contentWidth = Math.max(MIN_MEASURE_WIDTH, totalUnits * UNIT_WIDTH);
            double svgWidth = contentWidth + LEFT_MARGIN + RIGHT_MARGIN;
            double svgHeight = TOP_MARGIN + (labels.size() - 1) * STRING_SPACING + BOTTOM_MARGIN;
            double stringTop = TOP_MARGIN;
            double stringBottom = TOP_MARGIN + (labels.size() - 1) * STRING_SPACING;

            StringBuilder svg = new StringBuilder();
            svg.append("<div class=\"measure\">");

            String marker = measure.path("marker").path("text").asText(null);
            if (marker != null && !marker.isBlank()) {
                svg.append("<div class=\"measure-marker\">").append(escape(marker)).append("</div>");
            }

            svg.append("<svg class=\"tab-svg\" width=\"").append(format(svgWidth))
                    .append("\" height=\"").append(format(svgHeight))
                    .append("\" viewBox=\"0 0 ").append(format(svgWidth)).append(' ').append(format(svgHeight))
                    .append("\" xmlns=\"http://www.w3.org/2000/svg\">");

            // Strings and labels
            for (int i = 0; i < labels.size(); i++) {
                double y = stringTop + i * STRING_SPACING;
                svg.append("<line class=\"string-line\" x1=\"").append(format(LEFT_MARGIN))
                        .append("\" y1=\"").append(format(y))
                        .append("\" x2=\"").append(format(svgWidth - RIGHT_MARGIN))
                        .append("\" y2=\"").append(format(y))
                        .append("\"/>");
                svg.append("<text class=\"string-label\" x=\"")
                        .append(format(LEFT_MARGIN - 12))
                        .append("\" y=\"").append(format(y))
                        .append("\">")
                        .append(escape(labels.get(i)))
                        .append("</text>");
            }

            // Measure boundaries
            svg.append("<line class=\"measure-bar\" x1=\"").append(format(LEFT_MARGIN))
                    .append("\" y1=\"").append(format(stringTop - 12))
                    .append("\" x2=\"").append(format(LEFT_MARGIN))
                    .append("\" y2=\"").append(format(stringBottom + 16))
                    .append("\"/>");
            svg.append("<line class=\"measure-bar\" x1=\"").append(format(svgWidth - RIGHT_MARGIN))
                    .append("\" y1=\"").append(format(stringTop - 12))
                    .append("\" x2=\"").append(format(svgWidth - RIGHT_MARGIN))
                    .append("\" y2=\"").append(format(stringBottom + 16))
                    .append("\"/>");

            boolean repeatStart = measure.path("repeatStart").asBoolean(false);
            if (repeatStart) {
                appendRepeatStart(svg, LEFT_MARGIN, stringTop, stringBottom);
            }

            int repeatEnd = measure.path("repeat").asInt(0);
            if (repeatEnd > 0) {
                appendRepeatEnd(svg, svgWidth - RIGHT_MARGIN, stringTop, stringBottom, repeatEnd);
            }

            ArrayNode alternateEnding = asArray(measure.get("alternateEnding"));
            if (alternateEnding != null && !alternateEnding.isEmpty()) {
                svg.append("<text class=\"repeat-marker\" x=\"")
                        .append(format(LEFT_MARGIN))
                        .append("\" y=\"").append(format(stringTop - 36))
                        .append("\">")
                        .append(escape(formatAlternateEnding(alternateEnding)))
                        .append("</text>");
            }

            ArrayNode signature = asArray(measure.get("signature"));
            if (signature != null && signature.size() == 2) {
                svg.append("<text class=\"measure-signature\" x=\"")
                        .append(format(LEFT_MARGIN - 38))
                        .append("\" y=\"").append(format(stringTop - 18))
                        .append("\">")
                        .append(signature.get(0).asInt()).append('/').append(signature.get(1).asInt())
                        .append("</text>");
            }

            List<String> connectors = new ArrayList<>();
            List<String> overlays = new ArrayList<>();
            List<TupletMark> tuplets = new ArrayList<>();
            List<VibratoSegment> vibratoSegments = new ArrayList<>();
            VibratoSegment currentVibrato = null;
            final double vibratoBaseY = stringTop - 22;

            double cursor = 0.0;
            for (BeatInfo info : infos) {
                double beatWidth = Math.max(MIN_BEAT_WIDTH, info.units * UNIT_WIDTH);
                double beatLeft = LEFT_MARGIN + cursor;
                double beatRight = beatLeft + beatWidth;
                double centerX = beatLeft + beatWidth / 2.0;

                info.left = beatLeft;
                info.right = beatRight;
                info.center = centerX;
                info.width = beatWidth;

                double effectBaseY = stringTop - 22;
                int effectRow = 0;

                boolean beatRest = info.node.path("rest").asBoolean(false);
                boolean beatPalmMute = info.node.path("palmMute").asBoolean(false);
                boolean beatLetRing = info.node.path("letRing").asBoolean(false);
                String beatText = info.node.path("text").asText(null);
                String velocity = info.node.path("velocity").asText(null);
                boolean upStroke = info.node.path("upStroke").asBoolean(false);
                boolean downStroke = info.node.path("downStroke").asBoolean(false);
                VibratoStyle beatStyle = parseVibrato(info.node);

                if (beatPalmMute) {
                    overlays.add(effectLabel(centerX, effectBaseY - effectRow * 12, "P.M.", "effect-label muted"));
                    effectRow++;
                }
                if (beatLetRing) {
                    overlays.add(effectLabel(centerX, effectBaseY - effectRow * 12, "L.R.", "effect-label let-ring"));
                    effectRow++;
                }
                if (velocity != null && !velocity.isBlank()) {
                    overlays.add(effectLabel(centerX, effectBaseY - effectRow * 12, velocity, "effect-label dynamic"));
                    effectRow++;
                }
                if (beatText != null && !beatText.isBlank()) {
                    overlays.add(effectLabel(centerX, effectBaseY - effectRow * 12, beatText, "effect-label text"));
                    effectRow++;
                }

                if (upStroke || downStroke) {
                    overlays.add(buildStroke(centerX, stringBottom + 12, stringBottom + 28, upStroke));
                }

                if (info.node.path("tupletStart").asBoolean(false)) {
                    TupletMark mark = new TupletMark();
                    mark.startX = beatLeft;
                    mark.level = tuplets.size();
                    mark.count = info.node.path("tuplet").asInt(0);
                    tuplets.add(mark);
                }
                for (TupletMark mark : tuplets) {
                    mark.endX = beatRight;
                }
                if (info.node.path("tupletStop").asBoolean(false)) {
                    if (!tuplets.isEmpty()) {
                        TupletMark mark = tuplets.remove(tuplets.size() - 1);
                        overlays.add(buildTuplet(mark, stringTop));
                    }
                }

                ArrayNode notes = asArray(info.node.get("notes"));
                if (beatRest || notes == null || notes.isEmpty()) {
                    overlays.add(buildRestGlyph(info.node, centerX, stringTop, stringBottom));
                    if (currentVibrato != null) {
                        vibratoSegments.add(currentVibrato);
                        currentVibrato = null;
                    }
                    cursor += beatWidth;
                    continue;
                }

                VibratoStyle noteMaxStyle = beatStyle;
                for (JsonNode note : notes) {
                    if (note.path("rest").asBoolean(false)) {
                        continue;
                    }
                    if (!note.has("string")) {
                        continue;
                    }
                    int rawString = note.path("string").asInt(-1);
                    if (rawString < 0) {
                        continue;
                    }
                    int stringIndex = zeroBasedStrings ? rawString : rawString - 1;
                    if (stringIndex < 0) {
                        stringIndex = 0;
                    }
                    if (stringIndex >= labels.size()) {
                        stringIndex = labels.size() - 1;
                    }
                    double stringY = stringTop + stringIndex * STRING_SPACING;
                    int fret = note.path("fret").asInt(0);
                    boolean ghost = note.path("ghost").asBoolean(false);
                    boolean dead = note.path("dead").asBoolean(false);
                    boolean grace = note.has("graceNote");
                    boolean staccato = note.path("staccato").asBoolean(false);
                    VibratoStyle noteVibratoStyle = parseVibrato(note);
                    noteMaxStyle = maxStyle(noteMaxStyle, noteVibratoStyle);

                    String classes = "note";
                    if (grace) {
                        classes += " grace";
                    }
                    if (ghost) {
                        classes += " ghost";
                    }
                    if (dead) {
                        classes += " dead";
                    }

                    NotePosition previous = lastNotePerString.get(rawString);
                    boolean tie = note.path("tie").asBoolean(false);
                    boolean tieToSame = tie && previous != null && previous.fret == fret;

                    if (!tieToSame) {
                        String fretText = dead ? "x" : note.path("fret").asText();
                        svg.append("<text class=\"").append(classes)
                                .append("\" x=\"").append(format(centerX))
                                .append("\" y=\"").append(format(stringY))
                                .append("\">")
                                .append(escape(fretText))
                                .append("</text>");
                        if (staccato) {
                            overlays.add(buildStaccato(centerX, stringY - 10));
                        }
                    }

                    String slideKind = note.path("slide").asText(null);
                    if (slideKind != null && previous != null) {
                        connectors.add(buildSlide(previous, centerX, stringY, slideKind));
                    }

                    if (!tieToSame && note.has("bend")) {
                        connectors.addAll(buildBend(centerX, stringY, note.get("bend")));
                    }

                    if (tieToSame) {
                        connectors.add(buildTie(previous, centerX, stringY, true));
                    } else if (tie && previous != null) {
                        connectors.add(buildTie(previous, centerX, stringY, false));
                    }

                    boolean hp = note.path("hp").asBoolean(false);
                    if (!tieToSame && hp && previous != null) {
                        boolean hammer = fret >= previous.fret;
                        connectors.addAll(buildLegato(previous, centerX, stringY, hammer));
                    }

                    if (!tieToSame) {
                        double effectY = stringY - 18;
                        if (note.has("harmonic") || note.has("naturalHarmonic")) {
                            overlays.add(effectLabel(centerX, effectY, "N.H.", "effect-label accidental"));
                            effectY -= 12;
                        }
                        if (note.has("pinchHarmonic")) {
                            overlays.add(effectLabel(centerX, effectY, "P.H.", "effect-label accidental"));
                            effectY -= 12;
                        }
                        if (note.has("artificialHarmonic")) {
                            overlays.add(effectLabel(centerX, effectY, "A.H.", "effect-label accidental"));
                            effectY -= 12;
                        }
                        if (note.has("tapping") || note.has("tap")) {
                            overlays.add(effectLabel(centerX, effectY, "T", "effect-label accidental"));
                            effectY -= 12;
                        }
                        if (note.has("tremolo") || note.has("tremoloPicking")) {
                            overlays.add(effectLabel(centerX, effectY, "Tr", "effect-label accidental"));
                            effectY -= 12;
                        }
                    }

                    NotePosition current = new NotePosition(centerX, stringY, fret);
                    lastNotePerString.put(rawString, current);
                }

                if (noteMaxStyle != VibratoStyle.NONE) {
                    if (currentVibrato == null) {
                        currentVibrato = new VibratoSegment();
                        currentVibrato.style = noteMaxStyle;
                        currentVibrato.start = beatLeft;
                        currentVibrato.end = beatRight;
                    } else if (currentVibrato.style == noteMaxStyle) {
                        currentVibrato.end = beatRight;
                    } else {
                        vibratoSegments.add(currentVibrato);
                        currentVibrato = new VibratoSegment();
                        currentVibrato.style = noteMaxStyle;
                        currentVibrato.start = beatLeft;
                        currentVibrato.end = beatRight;
                    }
                } else {
                    if (currentVibrato != null) {
                        vibratoSegments.add(currentVibrato);
                        currentVibrato = null;
                    }
                }

                cursor += beatWidth;
            }

            // Close any unfinished tuplets
            for (TupletMark mark : tuplets) {
                overlays.add(buildTuplet(mark, stringTop));
            }
            if (currentVibrato != null) {
                vibratoSegments.add(currentVibrato);
            }

            for (VibratoSegment segment : vibratoSegments) {
                String path = buildVibratoRun(segment.start, segment.end, vibratoBaseY, segment.style);
                if (!path.isEmpty()) {
                    overlays.add(path);
                }
            }

            for (String connector : connectors) {
                svg.append(connector);
            }
            for (String overlay : overlays) {
                svg.append(overlay);
            }

            svg.append("</svg>");
            svg.append("</div>");
            html.append(svg);
        }

        private String renderEmptyMeasure(JsonNode measure, int index) {
            boolean rest = measure.path("rest").asBoolean(false);
            StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"measure\">");
            sb.append("<div class=\"measure-marker\">Bar ").append(index).append("</div>");
            if (rest) {
                sb.append("<div class=\"empty-state\">(rest measure)</div>");
            } else {
                sb.append("<div class=\"empty-state\">(blank measure)</div>");
            }
            sb.append("</div>");
            return sb.toString();
        }

        private String effectLabel(double x, double y, String text, String cssClass) {
            return "<text class=\"" + cssClass + "\" x=\"" + format(x) + "\" y=\"" + format(y) + "\">" + escape(text) + "</text>";
        }

        private void appendRepeatStart(StringBuilder svg, double x, double top, double bottom) {
            double inner = x + 6;
            svg.append("<line class=\"repeat-line\" x1=\"").append(format(inner))
                    .append("\" y1=\"").append(format(top - 12))
                    .append("\" x2=\"").append(format(inner))
                    .append("\" y2=\"").append(format(bottom + 16))
                    .append("\"/>");
            svg.append("<line class=\"repeat-line thin\" x1=\"").append(format(inner + 4))
                    .append("\" y1=\"").append(format(top - 12))
                    .append("\" x2=\"").append(format(inner + 4))
                    .append("\" y2=\"").append(format(bottom + 16))
                    .append("\"/>");
            double dotX = inner + 8;
            double dotTop = (top + bottom) / 2 - 6;
            svg.append("<circle class=\"repeat-dot\" cx=\"").append(format(dotX))
                    .append("\" cy=\"").append(format(dotTop))
                    .append("\" r=\"2.7\"/>");
            svg.append("<circle class=\"repeat-dot\" cx=\"").append(format(dotX))
                    .append("\" cy=\"").append(format(dotTop + 12))
                    .append("\" r=\"2.7\"/>");
        }

        private void appendRepeatEnd(StringBuilder svg, double x, double top, double bottom, int repeats) {
            double inner = x - 6;
            svg.append("<line class=\"repeat-line thin\" x1=\"").append(format(inner - 4))
                    .append("\" y1=\"").append(format(top - 12))
                    .append("\" x2=\"").append(format(inner - 4))
                    .append("\" y2=\"").append(format(bottom + 16))
                    .append("\"/>");
            svg.append("<line class=\"repeat-line\" x1=\"").append(format(inner))
                    .append("\" y1=\"").append(format(top - 12))
                    .append("\" x2=\"").append(format(inner))
                    .append("\" y2=\"").append(format(bottom + 16))
                    .append("\"/>");
            double dotX = inner - 8;
            double dotTop = (top + bottom) / 2 - 6;
            svg.append("<circle class=\"repeat-dot\" cx=\"").append(format(dotX))
                    .append("\" cy=\"").append(format(dotTop))
                    .append("\" r=\"2.7\"/>");
            svg.append("<circle class=\"repeat-dot\" cx=\"").append(format(dotX))
                    .append("\" cy=\"").append(format(dotTop + 12))
                    .append("\" r=\"2.7\"/>");
            svg.append("<text class=\"repeat-marker\" x=\"")
                    .append(format(inner - 14))
                    .append("\" y=\"").append(format(top - 20))
                    .append("\">×").append(repeats)
                    .append("</text>");
        }

        private List<String> buildLegato(NotePosition from, double toX, double toY, boolean hammer) {
            List<String> elements = new ArrayList<>();
            double controlX = (from.x + toX) / 2.0;
            double controlY = Math.min(from.y, toY) - 18;
            String path = "<path class=\"connection " + (hammer ? "hammer" : "pull") + "\" d=\"M "
                    + format(from.x) + " " + format(from.y)
                    + " Q " + format(controlX) + " " + format(controlY)
                    + " " + format(toX) + " " + format(toY) + "\"/>";
            elements.add(path);
            String label = hammer ? "H" : "P";
            elements.add(effectLabel(controlX, controlY - 4, label, "effect-label hp"));
            return elements;
        }

        private String buildSlide(NotePosition from, double toX, double toY, String slideKind) {
            boolean legato = "legato".equalsIgnoreCase(slideKind);
            double controlY = Math.min(from.y, toY) - (legato ? 12 : 4);
            return "<path class=\"connection slide" + (legato ? " legato" : "") + "\" d=\"M "
                    + format(from.x) + " " + format(from.y)
                    + " Q " + format((from.x + toX) / 2.0) + " " + format(controlY)
                    + " " + format(toX) + " " + format(toY) + "\"/>";
        }

        private String buildTie(NotePosition from, double toX, double toY, boolean samePitch) {
            double baseline = Math.max(from.y, toY);
            double offset = samePitch ? 18 : 14;
            double controlY = baseline + offset;
            return "<path class=\"connection tie\" d=\"M "
                    + format(from.x) + " " + format(from.y)
                    + " Q " + format((from.x + toX) / 2.0) + " " + format(controlY)
                    + " " + format(toX) + " " + format(toY) + "\"/>";
        }

        private List<String> buildBend(double centerX, double stringY, JsonNode bendNode) {
            List<String> elements = new ArrayList<>();
            ArrayNode points = asArray(bendNode.get("points"));
            int tone = bendNode.path("tone").asInt(0);
            int maxTone = tone;
            int lastTone = tone;
            if (points != null && !points.isEmpty()) {
                for (JsonNode point : points) {
                    int value = point.path("tone").asInt(0);
                    maxTone = Math.max(maxTone, value);
                    lastTone = value;
                }
            }
            boolean release = lastTone == 0 && maxTone > 0;
            double height = 18 + (maxTone / 25.0) * 10.0;
            double apexX = centerX + 16;
            double apexY = stringY - height;
            double control1X = centerX + 6;
            double control1Y = stringY - height * 0.35;
            double control2X = centerX + 12;
            double control2Y = stringY - height * 0.75;
            String path = "<path class=\"bend-path\" d=\"M "
                    + format(centerX) + " " + format(stringY)
                    + " C " + format(control1X) + " " + format(control1Y) + ", "
                    + format(control2X) + " " + format(control2Y) + ", "
                    + format(apexX) + " " + format(apexY) + "\"/>";
            elements.add(path);
            // arrow head
            elements.add("<path class=\"bend-path\" d=\"M "
                    + format(apexX) + " " + format(apexY)
                    + " l -5 6\"/>");
            elements.add("<path class=\"bend-path\" d=\"M "
                    + format(apexX) + " " + format(apexY)
                    + " l 5 6\"/>");

            String label = formatBendLabel(maxTone);
            elements.add("<text class=\"bend-label\" x=\""
                    + format(apexX + 6) + "\" y=\"" + format(apexY - 4) + "\">"
                    + label + "</text>");

            if (release) {
                double releaseX = apexX + 12;
                double releaseY = stringY;
                elements.add("<path class=\"bend-path release\" d=\"M "
                        + format(apexX) + " " + format(apexY)
                        + " C " + format(apexX + 4) + " " + format(apexY - 6) + ", "
                        + format(releaseX - 4) + " " + format(releaseY - 14) + ", "
                        + format(releaseX) + " " + format(releaseY) + "\"/>");
                elements.add("<text class=\"bend-text\" x=\""
                        + format(releaseX - 2) + "\" y=\"" + format(releaseY - 6)
                        + "\">rel.</text>");
            }

            return elements;
        }

        private String buildTuplet(TupletMark mark, double stringTop) {
            double y = stringTop - 26 - mark.level * 12;
            double start = mark.startX;
            double end = mark.endX;
            double mid = (start + end) / 2.0;
            StringBuilder sb = new StringBuilder();
            sb.append("<path class=\"tuplet-bracket\" d=\"M ")
                    .append(format(start)).append(" ").append(format(y))
                    .append(" L ").append(format(start + 6)).append(" ").append(format(y - 6))
                    .append(" L ").append(format(end - 6)).append(" ").append(format(y - 6))
                    .append(" L ").append(format(end)).append(" ").append(format(y))
                    .append("\"/>");
            if (mark.count > 0) {
                sb.append("<text class=\"tuplet-label\" x=\"")
                        .append(format(mid))
                        .append("\" y=\"").append(format(y - 8))
                        .append("\">").append(mark.count).append("</text>");
            }
            return sb.toString();
        }

        private String buildRestGlyph(JsonNode beat, double centerX, double stringTop, double stringBottom) {
            int type = beat.path("type").asInt(4);
            switch (type) {
                case 1: // whole
                    return "<rect class=\"rest-rect\" x=\"" + format(centerX - 10)
                            + "\" y=\"" + format(stringTop - 10)
                            + "\" width=\"20\" height=\"4\"/>";
                case 2: // half
                    return "<rect class=\"rest-rect\" x=\"" + format(centerX - 10)
                            + "\" y=\"" + format(stringTop - 16)
                            + "\" width=\"20\" height=\"4\"/>";
                case 8:
                case 16:
                case 32:
                case 64:
                    return buildEighthRest(centerX, stringTop - 8, type);
                case 4:
                default:
                    return buildQuarterRest(centerX, stringTop - 6);
            }
        }

        private String buildQuarterRest(double x, double topY) {
            double y1 = topY;
            return "<path class=\"rest-shape\" d=\"M "
                    + format(x) + " " + format(y1)
                    + " l 5 8 l -8 10 l 6 10 l -5 8\"/>";
        }

        private String buildEighthRest(double x, double topY, int type) {
            double flagOffset = (type == 8) ? 0 : (type == 16 ? 7 : 12);
            StringBuilder sb = new StringBuilder();
            sb.append("<path class=\"rest-shape\" d=\"M ")
                    .append(format(x)).append(" ").append(format(topY))
                    .append(" q 6 8 0 14 q -6 8 0 14\"/>");
            sb.append("<circle class=\"rest-rect\" cx=\"")
                    .append(format(x + 6)).append("\" cy=\"")
                    .append(format(topY + 6)).append("\" r=\"3\"/>");
            if (type >= 16) {
                sb.append("<circle class=\"rest-rect\" cx=\"")
                        .append(format(x + 6)).append("\" cy=\"")
                        .append(format(topY + 12 + flagOffset)).append("\" r=\"3\"/>");
            }
            if (type >= 32) {
                sb.append("<circle class=\"rest-rect\" cx=\"")
                        .append(format(x + 6)).append("\" cy=\"")
                        .append(format(topY + 18 + flagOffset)).append("\" r=\"3\"/>");
            }
            if (type >= 64) {
                sb.append("<circle class=\"rest-rect\" cx=\"")
                        .append(format(x + 6)).append("\" cy=\"")
                        .append(format(topY + 24 + flagOffset)).append("\" r=\"3\"/>");
            }
            return sb.toString();
        }

        private String buildStaccato(double x, double y) {
            return "<circle class=\"staccato-dot\" cx=\"" + format(x) + "\" cy=\"" + format(y) + "\" r=\"2.6\"/>";
        }

        private String buildStroke(double x, double startY, double endY, boolean upStroke) {
            double headY = upStroke ? startY - 12 : endY + 12;
            double bodyStart = upStroke ? endY : startY;
            double bodyEnd = upStroke ? startY : endY;
            StringBuilder sb = new StringBuilder();
            sb.append("<line class=\"stroke-arrow").append(upStroke ? "" : " down")
                    .append("\" x1=\"").append(format(x)).append("\" y1=\"").append(format(bodyStart))
                    .append("\" x2=\"").append(format(x)).append("\" y2=\"").append(format(bodyEnd))
                    .append("\"/>");
            double headOffset = upStroke ? -6 : 6;
            sb.append("<path class=\"stroke-arrow-head").append(upStroke ? "" : " down")
                    .append("\" d=\"M ").append(format(x)).append(" ").append(format(headY))
                    .append(" l -6 ").append(format(headOffset))
                    .append(" h 12 z\"/>");
            return sb.toString();
        }

        private String buildVibratoRun(double startX, double endX, double baseY, VibratoStyle style) {
            if (endX <= startX) {
                return "";
            }
            double amplitude = vibratoAmplitude(style);
            double step = vibratoStep(style);
            String css = "vibrato-wave";
            if (style == VibratoStyle.WIDE) {
                css += " wide";
            } else if (style == VibratoStyle.WHAMMY) {
                css += " whammy";
            }
            StringBuilder path = new StringBuilder();
            path.append("<path class=\"").append(css).append("\" d=\"M ")
                    .append(format(startX)).append(" ").append(format(baseY)).append(" ");
            double x = startX;
            boolean up = true;
            while (x < endX - 0.5) {
                double next = Math.min(endX, x + step);
                double controlX = x + (next - x) / 2.0;
                double controlY = up ? baseY - amplitude : baseY + amplitude;
                path.append("Q ")
                        .append(format(controlX)).append(" ").append(format(controlY)).append(" ")
                        .append(format(next)).append(" ").append(format(baseY)).append(" ");
                x = next;
                up = !up;
            }
            path.append("\"/>");
            return path.toString();
        }

        private String formatAlternateEnding(ArrayNode endings) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < endings.size(); i++) {
                if (i > 0) sb.append('-');
                sb.append(endings.get(i).asInt());
            }
            sb.append('.');
            return sb.toString();
        }

        private String formatBendLabel(int tone) {
            if (tone <= 0) {
                return "bend";
            }
            int quarters = tone / 25;
            int whole = quarters / 4;
            int remainder = quarters % 4;
            StringBuilder sb = new StringBuilder();
            if (whole > 0) {
                sb.append(whole);
            }
            switch (remainder) {
                case 1:
                    sb.append("¼");
                    break;
                case 2:
                    sb.append("½");
                    break;
                case 3:
                    sb.append("¾");
                    break;
                default:
                    break;
            }
            if (sb.length() == 0) {
                sb.append("¼");
            }
            return sb.toString();
        }

        private String format(double value) {
            return String.format(LOCALE, "%.2f", value);
        }
    }

    private enum VibratoStyle {
        NONE,
        NORMAL,
        WIDE,
        WHAMMY
    }

    private static VibratoStyle parseVibrato(JsonNode node) {
        JsonNode vibratoNode = node.get("vibrato");
        if (vibratoNode == null || vibratoNode.isNull()) {
            return VibratoStyle.NONE;
        }
        if (vibratoNode.isBoolean()) {
            return vibratoNode.asBoolean(false) ? VibratoStyle.NORMAL : VibratoStyle.NONE;
        }
        if (vibratoNode.isTextual()) {
            String value = vibratoNode.asText("").toLowerCase(Locale.ROOT);
            if ("wide".equals(value) || "heavy".equals(value)) {
                return VibratoStyle.WIDE;
            }
            if ("whammy".equals(value) || "tremolo".equals(value)) {
                return VibratoStyle.WHAMMY;
            }
            if ("slight".equals(value) || "light".equals(value) || "vibrato".equals(value)) {
                return VibratoStyle.NORMAL;
            }
            return VibratoStyle.NORMAL;
        }
        return VibratoStyle.NORMAL;
    }

    private static VibratoStyle maxStyle(VibratoStyle a, VibratoStyle b) {
        return vibratoPriority(b) > vibratoPriority(a) ? b : a;
    }

    private static int vibratoPriority(VibratoStyle style) {
        switch (style) {
            case WHAMMY:
                return 3;
            case WIDE:
                return 2;
            case NORMAL:
                return 1;
            case NONE:
            default:
                return 0;
        }
    }

    private static double vibratoAmplitude(VibratoStyle style) {
        switch (style) {
            case WHAMMY:
                return 9.0;
            case WIDE:
                return 7.0;
            case NORMAL:
            default:
                return 4.5;
        }
    }

    private static double vibratoStep(VibratoStyle style) {
        switch (style) {
            case WHAMMY:
                return 20.0;
            case WIDE:
                return 16.0;
            case NORMAL:
            default:
                return 13.0;
        }
    }

    private static final class BeatInfo {
        final JsonNode node;
        final double units;
        double left;
        double right;
        double center;
        double width;

        BeatInfo(JsonNode node, double units) {
            this.node = node;
            this.units = units;
        }
    }

    private static final class VibratoSegment {
        double start;
        double end;
        VibratoStyle style;
    }

    private static final class NotePosition {
        final double x;
        final double y;
        final int fret;

        NotePosition(double x, double y, int fret) {
            this.x = x;
            this.y = y;
            this.fret = fret;
        }
    }

    private static final class TupletMark {
        double startX;
        double endX;
        int count;
        int level;
    }
}
