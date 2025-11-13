package com.shredforge.tab;

import com.shredforge.model.Tab;
import com.shredforge.model.Note;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for fetching tabs from Songsterr API.
 * Handles HTTP communication and JSON parsing.
 */
public class TabGetService {
    private static final Logger LOGGER = Logger.getLogger(TabGetService.class.getName());
    private static final String API_BASE_URL = "https://www.songsterr.com/a/ra";
    private static final String API_SONGS_ENDPOINT = "/songs.json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TabGetService() {
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for tabs on Songsterr
     * @param query Search query
     * @return List of tab metadata
     */
    public List<Tab> searchOnline(String query) {
        List<Tab> results = new ArrayList<>();

        try {
            String url = API_BASE_URL + API_SONGS_ENDPOINT + "?pattern=" +
                        java.net.URLEncoder.encode(query, "UTF-8");

            LOGGER.info("Fetching from Songsterr: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Tab tab = parseTabMetadata(node);
                        if (tab != null) {
                            results.add(tab);
                        }
                    }
                }

                LOGGER.info("Found " + results.size() + " tabs for query: " + query);
            } else {
                LOGGER.warning("Songsterr API returned status: " + response.statusCode());
            }

        } catch (Exception e) {
            LOGGER.severe("Failed to search tabs: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Download full tab details by ID
     */
    public Tab downloadTab(String tabId) {
        try {
            // For now, return a mock tab with sample data
            // In a real implementation, this would fetch detailed tab data
            LOGGER.info("Downloading full tab details for ID: " + tabId);

            Tab tab = new Tab(tabId, "Sample Tab", "Sample Artist");
            tab.setDifficulty("Medium");
            tab.setRating(4.0f);
            tab.setTempo(120);
            tab.setDuration(180000); // 3 minutes

            // Add some sample notes for testing
            List<Note> notes = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                notes.add(new Note("E", 2, 6, 0, i * 1000L));
            }
            tab.setNotes(notes);

            return tab;

        } catch (Exception e) {
            LOGGER.severe("Failed to download tab: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse tab metadata from JSON
     */
    private Tab parseTabMetadata(JsonNode node) {
        try {
            String id = node.has("id") ? node.get("id").asText() : String.valueOf(node.hashCode());
            String title = node.has("title") ? node.get("title").asText() : "Unknown";
            String artist = node.has("artist") ? node.get("artist").asText() : "Unknown";

            Tab tab = new Tab(id, title, artist);

            if (node.has("difficulty")) {
                tab.setDifficulty(node.get("difficulty").asText());
            } else {
                tab.setDifficulty("Medium");
            }

            if (node.has("rating")) {
                tab.setRating((float) node.get("rating").asDouble());
            } else {
                tab.setRating(3.5f);
            }

            tab.setDownloaded(false);

            return tab;

        } catch (Exception e) {
            LOGGER.warning("Failed to parse tab metadata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get tab metadata by ID
     */
    public Tab getTabMetadata(String tabId) {
        // Simplified implementation
        LOGGER.info("Fetching metadata for tab ID: " + tabId);
        return new Tab(tabId, "Sample Tab", "Sample Artist");
    }
}
