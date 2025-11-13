package com.shredforge.tabs.model;

public record TabSearchResult(TabSelection selection, boolean alreadySaved) {

    public String summary() {
        String status = alreadySaved ? "saved" : "new";
        return selection.displayLabel() + " [" + status + "]";
    }
}
