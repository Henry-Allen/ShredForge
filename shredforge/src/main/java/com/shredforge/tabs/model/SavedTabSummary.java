package com.shredforge.tabs.model;

import java.nio.file.Path;
import java.time.Instant;

public record SavedTabSummary(TabSelection selection, Instant savedAt, Path location) {

    public String tabId() {
        return selection.tabId();
    }
}
