package com.shredforge.tabs.model;

import java.util.Objects;

public record TabSearchRequest(String term) {

    public TabSearchRequest {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("Search term must not be blank.");
        }
    }

    public String cleanedTerm() {
        return term.trim();
    }
}
