package com.shredforge.tabview;

import com.shredforge.core.model.FormattedTab;
import com.shredforge.tabview.render.SongsterrTabFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight controller that manages navigating through SVG fragments for playback/preview.
 */
public final class TabPlaybackController {

    private FormattedTab currentTab;
    private int sectionIndex;

    public void load(FormattedTab tab) {
        this.currentTab = Objects.requireNonNull(tab, "tab");
        this.sectionIndex = 0;
    }

    public boolean hasTab() {
        return currentTab != null && (!currentTab.svgFragments().isEmpty() || !currentTab.documentHtml().isBlank());
    }

    public int sectionCount() {
        return hasTab() ? currentTab.svgFragments().size() : 0;
    }

    public int currentSectionIndex() {
        return sectionIndex;
    }

    public void nextSection() {
        if (sectionCount() == 0) {
            return;
        }
        sectionIndex = (sectionIndex + 1) % sectionCount();
    }

    public void previousSection() {
        if (sectionCount() == 0) {
            return;
        }
        sectionIndex = (sectionIndex - 1 + sectionCount()) % sectionCount();
    }

    public String currentSectionHtml() {
        if (!hasTab()) {
            return SongsterrTabFormatter.renderMessage("No tab data loaded yet.");
        }
        List<String> fragments = currentTab.svgFragments();
        if (fragments.isEmpty()) {
            return currentTab.documentHtml();
        }
        int safeIndex = Math.min(sectionIndex, fragments.size() - 1);
        return fragments.get(Math.max(0, safeIndex));
    }

    public String fullDocumentHtml() {
        if (!hasTab()) {
            return SongsterrTabFormatter.renderMessage("No tab data loaded yet.");
        }
        if (!currentTab.documentHtml().isBlank()) {
            return currentTab.documentHtml();
        }
        return SongsterrTabFormatter.wrapHtml("<div class=\"tab-container\"></div>");
    }
}
