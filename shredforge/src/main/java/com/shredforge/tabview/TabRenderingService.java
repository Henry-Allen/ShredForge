package com.shredforge.tabview;

import com.shredforge.core.model.FormattedTab;
import com.shredforge.core.model.TabData;
import com.shredforge.core.ports.TabFormatter;
import com.shredforge.tabview.render.SongsterrTabFormatter;
import java.util.List;

/**
 * Formats Songsterr JSON into SVG-based HTML documents that can be played back in the UI.
 */
public final class TabRenderingService implements TabFormatter {

    @Override
    public FormattedTab format(TabData tabData) {
        String document = SongsterrTabFormatter.render(tabData.rawContent(), List.of(), null);
        List<String> fragments = SongsterrTabFormatter.extractMeasureFragments(document);
        if (fragments.isEmpty()) {
            fragments = List.of(document);
        }
        return new FormattedTab(tabData.song(), fragments, document);
    }
}
