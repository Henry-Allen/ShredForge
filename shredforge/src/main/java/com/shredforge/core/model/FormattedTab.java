package com.shredforge.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Tab rendered into SVG fragments ready for display.
 */
public record FormattedTab(SongRequest song, List<String> svgFragments, String documentHtml) {

    public FormattedTab {
        Objects.requireNonNull(song, "song");
        svgFragments = svgFragments == null ? List.of() : List.copyOf(svgFragments);
        documentHtml = documentHtml == null ? "" : documentHtml;
    }

    public boolean isEmpty() {
        return svgFragments.isEmpty() && documentHtml.isBlank();
    }
}
