package com.shredforge.tabs.dao;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shredforge.tabs.model.SongSelection;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Data access object that hides Songsterr HTTP calls and saved-tab persistence.
 */
public final class TabDataDao {

    private static final String SONGSTERR_SEARCH_ENDPOINT = "https://www.songsterr.com/api/songs?pattern=";
    private static final String USER_AGENT = "Shredforge/0.1 (+https://github.com)";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final String GP_STORAGE_DIR = ".shredforge/gp-files";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Path gpStorageDir;

    public TabDataDao() {
        this(defaultGpStoragePath());
    }

    public TabDataDao(Path gpStorageDir) {
        this.gpStorageDir = gpStorageDir == null ? defaultGpStoragePath() : gpStorageDir;
        try {
            Files.createDirectories(this.gpStorageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create GP storage directory: " + this.gpStorageDir, ex);
        }
    }

    public List<SongSearchResult> searchSongs(String term) {
        try {
            String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(SONGSTERR_SEARCH_ENDPOINT + encoded))
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Songsterr search returned HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray()) {
                return Collections.emptyList();
            }
            List<SongSearchResult> results = new ArrayList<>();
            for (JsonNode node : root) {
                int songId = node.path("songId").asInt(-1);
                if (songId <= 0) {
                    continue;
                }
                String artist = node.path("artist").asText("Unknown Artist");
                String title = node.path("title").asText("Untitled");
                List<TrackSummary> trackSummaries = new ArrayList<>();
                JsonNode tracksNode = node.path("tracks");
                if (tracksNode.isArray()) {
                    int index = 0;
                    for (JsonNode trackNode : tracksNode) {
                        String hash = trackNode.path("hash").asText(null);
                        if (hash == null || hash.isEmpty()) {
                            index++;
                            continue;
                        }
                        String instrument = trackNode.path("instrument").asText("Unknown Instrument");
                        String name = trackNode.path("name").asText(instrument);
                        int difficulty = trackNode.path("difficulty").asInt(-1);
                        List<Integer> tuning = new ArrayList<>();
                        JsonNode tuningNode = trackNode.path("tuning");
                        if (tuningNode.isArray()) {
                            tuningNode.forEach(t -> tuning.add(t.asInt()));
                        }
                        trackSummaries.add(new TrackSummary(index, hash, name, instrument, difficulty, tuning));
                        index++;
                    }
                }
                results.add(new SongSearchResult(songId, artist, title, trackSummaries));
            }
            return results;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Songsterr search interrupted.", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to search Songsterr: " + ex.getMessage(), ex);
        }
    }

    /**
     * Downloads a Guitar Pro file for the given song selection using direct HTTP.
     * 
     * <p>This fetches the song revision data from Songsterr's API to get the GP file URL,
     * then downloads the file directly via HTTP.
     * 
     * @param selection the song to download
     * @return a CompletableFuture that resolves to the path of the downloaded GP file
     */
    public CompletableFuture<Path> downloadGpFile(SongSelection selection) {
        Objects.requireNonNull(selection, "selection");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path targetFile = buildGpTargetPath(selection);
                
                // Check if already cached
                if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
                    System.out.println("Using cached GP file: " + targetFile);
                    return targetFile;
                }
                
                // Fetch song revision to get the GP file URL
                String revisionUrl = "https://www.songsterr.com/api/meta/" + selection.songId() + "/revisions";
                System.out.println("Fetching revision data: " + revisionUrl);
                
                HttpRequest revisionRequest = HttpRequest.newBuilder()
                        .uri(URI.create(revisionUrl))
                        .header("User-Agent", USER_AGENT)
                        .timeout(HTTP_TIMEOUT)
                        .GET()
                        .build();
                
                HttpResponse<String> revisionResponse = httpClient.send(revisionRequest, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (revisionResponse.statusCode() != 200) {
                    throw new IOException("Failed to fetch revision data: HTTP " + revisionResponse.statusCode());
                }
                
                // Parse revision response to get the source URL
                JsonNode revisions = mapper.readTree(revisionResponse.body());
                if (!revisions.isArray() || revisions.isEmpty()) {
                    throw new IOException("No revisions found for song " + selection.songId());
                }
                
                // Get the latest revision's source URL
                JsonNode latestRevision = revisions.get(0);
                String sourceUrl = null;
                
                // Try to find the GP file URL in the revision data
                if (latestRevision.has("source")) {
                    sourceUrl = latestRevision.get("source").asText();
                } else if (latestRevision.has("guitarProTab")) {
                    sourceUrl = latestRevision.get("guitarProTab").asText();
                }
                
                if (sourceUrl == null || sourceUrl.isBlank()) {
                    // Fallback: construct URL from known CDN pattern
                    int revisionId = latestRevision.has("revisionId") 
                            ? latestRevision.get("revisionId").asInt() 
                            : selection.songId();
                    sourceUrl = "https://gp.songsterr.com/gp/e/" + revisionId;
                }
                
                System.out.println("Downloading GP file from: " + sourceUrl);
                
                // Download the GP file
                HttpRequest downloadRequest = HttpRequest.newBuilder()
                        .uri(URI.create(sourceUrl))
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", "https://www.songsterr.com/")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
                
                HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest,
                        HttpResponse.BodyHandlers.ofByteArray());
                
                if (downloadResponse.statusCode() != 200) {
                    throw new IOException("Failed to download GP file: HTTP " + downloadResponse.statusCode());
                }
                
                byte[] gpData = downloadResponse.body();
                if (gpData.length == 0) {
                    throw new IOException("Downloaded GP file is empty");
                }
                
                // Save to disk
                Files.write(targetFile, gpData);
                System.out.println("GP file saved: " + targetFile + " (" + gpData.length + " bytes)");
                
                return targetFile;
                
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Download interrupted", ex);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to download GP file: " + ex.getMessage(), ex);
            }
        });
    }

    private static Path defaultGpStoragePath() {
        return Paths.get(System.getProperty("user.home")).resolve(GP_STORAGE_DIR);
    }

    private Path buildGpTargetPath(SongSelection selection) {
        String filename = sanitize(selection.songId() + "_" + selection.artist() + "_" + selection.title()) + ".gp";
        return gpStorageDir.resolve(filename);
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "_").replaceAll("_+", "_");
    }

    /**
     * Returns the GP storage directory.
     */
    public Path getGpStorageDir() {
        return gpStorageDir;
    }

    // --- Search result records ---

    public record SongSearchResult(int songId, String artist, String title, List<TrackSummary> tracks) {}

    public record TrackSummary(int index, String hash, String name, String instrument, int difficulty, List<Integer> tuning) {}
}
