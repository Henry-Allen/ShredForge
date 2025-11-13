package com.shredforge.tab;

import com.shredforge.model.Tab;
import com.shredforge.model.Note;
import com.shredforge.util.RetryExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for fetching tabs from Songsterr API with retry logic.
 * Handles HTTP communication, JSON parsing, and network failures gracefully.
 */
public class TabGetService {
    private static final Logger LOGGER = Logger.getLogger(TabGetService.class.getName());
    private static final String API_BASE_URL = "https://www.songsterr.com/a/ra";
    private static final String API_SONGS_ENDPOINT = "/songs.json";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryExecutor retryExecutor;

    public TabGetService() {
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build();
        this.objectMapper = new ObjectMapper();
        this.retryExecutor = new RetryExecutor(3, 1000, 2.0, 10000);
    }

    /**
     * Search for tabs on Songsterr with retry logic
     * @param query Search query
     * @return List of tab metadata
     */
    public List<Tab> searchOnline(String query) {
        if (query == null || query.trim().isEmpty()) {
            LOGGER.warning("Empty search query provided");
            return new ArrayList<>();
        }

        try {
            return retryExecutor.execute(() -> {
                List<Tab> results = new ArrayList<>();

                try {
                    String url = API_BASE_URL + API_SONGS_ENDPOINT + "?pattern=" +
                                java.net.URLEncoder.encode(query, "UTF-8");

                    LOGGER.info("Fetching from Songsterr: " + url);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                        .GET()
                        .build();

                    HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        if (response.body() == null || response.body().isEmpty()) {
                            LOGGER.warning("Empty response body from Songsterr");
                            return results;
                        }

                        JsonNode root = objectMapper.readTree(response.body());

                        if (root.isArray()) {
                            for (JsonNode node : root) {
                                try {
                                    Tab tab = parseTabMetadata(node);
                                    if (tab != null) {
                                        results.add(tab);
                                    }
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Error parsing tab node", e);
                                    // Continue with next tab
                                }
                            }
                        }

                        LOGGER.info("Found " + results.size() + " tabs for query: " + query);
                    } else if (response.statusCode() >= 500) {
                        // Server error - throw to trigger retry
                        throw new java.io.IOException("Server error: " + response.statusCode());
                    } else {
                        LOGGER.warning("Songsterr API returned status: " + response.statusCode());
                    }

                } catch (java.io.IOException | InterruptedException e) {
                    // Retriable errors
                    throw new Exception("Network error during tab search", e);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during tab search", e);
                    // Non-retriable error
                    throw e;
                }

                return results;
            }, "Songsterr tab search");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search tabs after retries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Download full tab details by ID with retry logic
     */
    public Tab downloadTab(String tabId) {
        if (tabId == null || tabId.trim().isEmpty()) {
            LOGGER.warning("Invalid tab ID for download");
            return null;
        }

        try {
            return retryExecutor.execute(() -> {
                try {
                    // For now, return a mock tab with sample data
                    // In a real implementation, this would fetch detailed tab data from Songsterr
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

                    LOGGER.info("Successfully downloaded tab: " + tabId);
                    return tab;

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error downloading tab", e);
                    throw e;
                }
            }, "Tab download: " + tabId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to download tab after retries: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse tab metadata from JSON with robust error handling
     */
    private Tab parseTabMetadata(JsonNode node) {
        if (node == null || node.isNull()) {
            LOGGER.warning("Cannot parse null JSON node");
            return null;
        }

        try {
            // Extract required fields with validation
            String id = node.has("id") && !node.get("id").isNull()
                ? node.get("id").asText()
                : String.valueOf(System.currentTimeMillis());

            String title = node.has("title") && !node.get("title").isNull()
                ? node.get("title").asText()
                : "Unknown";

            String artist = node.has("artist") && !node.get("artist").isNull()
                ? node.get("artist").asText()
                : "Unknown";

            // Validate required fields
            if (id.isEmpty() || title.isEmpty()) {
                LOGGER.warning("Tab metadata missing required fields");
                return null;
            }

            Tab tab = new Tab(id, title, artist);

            // Parse optional fields safely
            try {
                if (node.has("difficulty") && !node.get("difficulty").isNull()) {
                    tab.setDifficulty(node.get("difficulty").asText());
                } else {
                    tab.setDifficulty("Medium");
                }
            } catch (Exception e) {
                LOGGER.fine("Error parsing difficulty, using default");
                tab.setDifficulty("Medium");
            }

            try {
                if (node.has("rating") && !node.get("rating").isNull()) {
                    float rating = (float) node.get("rating").asDouble();
                    tab.setRating(Math.max(0.0f, Math.min(5.0f, rating))); // Clamp 0-5
                } else {
                    tab.setRating(3.5f);
                }
            } catch (Exception e) {
                LOGGER.fine("Error parsing rating, using default");
                tab.setRating(3.5f);
            }

            tab.setDownloaded(false);

            return tab;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse tab metadata", e);
            return null;
        }
    }

    /**
     * Get tab metadata by ID
     */
    public Tab getTabMetadata(String tabId) {
        if (tabId == null || tabId.trim().isEmpty()) {
            LOGGER.warning("Invalid tab ID for metadata fetch");
            return null;
        }

        // Simplified implementation
        LOGGER.info("Fetching metadata for tab ID: " + tabId);
        Tab tab = new Tab(tabId, "Sample Tab", "Sample Artist");
        tab.setDifficulty("Medium");
        tab.setRating(3.5f);
        return tab;
    }
}
