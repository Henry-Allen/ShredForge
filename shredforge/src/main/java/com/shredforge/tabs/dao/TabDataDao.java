package com.shredforge.tabs.dao;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.tabs.model.TabSelection;
import com.shredforge.tabs.util.TuningFormatter;
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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Data access object that hides Songsterr HTTP calls and saved-tab persistence.
 */
public final class TabDataDao {

    private static final String SONGSTERR_SEARCH_ENDPOINT = "https://www.songsterr.com/api/songs?pattern=";
    private static final String SONGSTERR_VIEW_BASE = "https://www.songsterr.com/a/wsa/";
    private static final String[] PART_CDN_HOSTS = {"d3rrfvx08uyjp1", "dodkcbujl0ebx", "dj1usja78sinh"};
    private static final String USER_AGENT = "Shredforge/0.1 (+https://github.com)";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final String STORAGE_DIR_NAME = ".shredforge/tabs";
    private static final String STORAGE_EXTENSION = ".json";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Path storageDir;

    public TabDataDao() {
        this(defaultStoragePath());
    }

    public TabDataDao(Path storageDir) {
        this.storageDir = storageDir == null ? defaultStoragePath() : storageDir;
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create tab storage directory: " + this.storageDir, ex);
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

    public SongDetails fetchSongDetails(TabSelection selection) {
        Objects.requireNonNull(selection, "selection");
        try {
            String songUrl = buildSongUrl(selection.artist(), selection.title(), selection.songId());
            HttpRequest request = HttpRequest.newBuilder(URI.create(songUrl))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Song view returned HTTP " + response.statusCode());
            }
            JsonNode stateJson = extractStateJson(response.body());
            JsonNode meta = stateJson.path("meta").path("current");
            int revisionId = meta.path("revisionId").asInt(-1);
            if (revisionId <= 0) {
                throw new IOException("Songsterr response missing revision id.");
            }
            Map<String, TrackMeta> trackMap = new HashMap<>();
            JsonNode tracksNode = meta.path("tracks");
            if (tracksNode.isArray()) {
                int index = 0;
                for (JsonNode trackNode : tracksNode) {
                    String hash = trackNode.path("hash").asText(null);
                    int partId = trackNode.path("partId").asInt(-1);
                    if (hash != null && partId >= 0) {
                        trackMap.put(hash, new TrackMeta(partId, index));
                    }
                    index++;
                }
            }
            return new SongDetails(selection.songId(), revisionId, trackMap);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading song details.", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load song details: " + ex.getMessage(), ex);
        }
    }

    public String fetchTabJson(int revisionId, int partId) {
        List<URI> candidates = buildCandidateUris(revisionId, partId);
        for (URI uri : candidates) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    return response.body();
                }
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while fetching tab JSON.", interrupted);
                }
            }
        }
        throw new IllegalStateException("No Songsterr tab sources responded.");
    }

    public SavedTabDto saveTab(TabSelection selection, TabData tabData) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(tabData, "tabData");
        try {
            StoredTab stored = new StoredTab();
            stored.tabId = selection.tabId();
            stored.selection = selection;
            stored.song = tabData.song();
            stored.rawContent = tabData.rawContent();
            stored.fetchedAt = tabData.fetchedAt();
            stored.savedAt = Instant.now();
            // Ensure storage directory still exists and is writable (in case external state changed)
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            if (!Files.isDirectory(storageDir)) {
                throw new IOException("Storage path is not a directory: " + storageDir);
            }
            if (!Files.isWritable(storageDir)) {
                throw new IOException("Storage directory is not writable: " + storageDir);
            }
            Path file = storageDir.resolve(sanitize(stored.tabId) + STORAGE_EXTENSION);
            Path tempFile = Files.createTempFile(storageDir, "tab-", ".json.tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), stored);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            return new SavedTabDto(
                    stored.tabId, selection, tabData.song(), tabData.rawContent(), tabData.fetchedAt(), stored.savedAt, file);
        } catch (IOException ex) {
            // Include more actionable context to aid debugging (target path and root cause)
            String target = storageDir.resolve(sanitize(selection.tabId()) + STORAGE_EXTENSION).toString();
            throw new IllegalStateException("Failed to save tab data to " + target + ": " + ex.getMessage(), ex);
        }
    }

    public List<SavedTabDto> listSavedTabs() {
        if (!Files.isDirectory(storageDir)) {
            return List.of();
        }
        try {
            List<SavedTabDto> results = new ArrayList<>();
            try (var stream = Files.list(storageDir)) {
                stream.filter(path -> path.toString().endsWith(STORAGE_EXTENSION))
                        .sorted()
                        .forEach(path -> loadStoredTab(path).ifPresent(results::add));
            }
            return results;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to enumerate saved tabs.", ex);
        }
    }

    public Optional<SavedTabDto> loadSavedTab(String tabId) {
        Path file = storageDir.resolve(sanitize(tabId) + STORAGE_EXTENSION);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return loadStoredTab(file);
    }

    private Optional<SavedTabDto> loadStoredTab(Path file) {
        try {
            StoredTab stored = mapper.readValue(file.toFile(), StoredTab.class);
            return Optional.of(new SavedTabDto(
                    stored.tabId, stored.selection, stored.song, stored.rawContent, stored.fetchedAt, stored.savedAt, file));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static Path defaultStoragePath() {
        return Paths.get(System.getProperty("user.home")).resolve(STORAGE_DIR_NAME);
    }

    private static String buildSongUrl(String artist, String title, int songId) {
        String artistSlug = TuningFormatter.slugify(artist);
        String titleSlug = TuningFormatter.slugify(title);
        return SONGSTERR_VIEW_BASE + artistSlug + "-" + titleSlug + "-tab-s" + songId;
    }

    private JsonNode extractStateJson(String html) throws IOException {
        int marker = html.indexOf("<script id=\"state\"");
        if (marker == -1) {
            throw new IOException("Unable to locate Songsterr state payload.");
        }
        int start = html.indexOf('>', marker);
        int end = html.indexOf("</script>", start);
        if (start == -1 || end == -1) {
            throw new IOException("Malformed Songsterr state script.");
        }
        String jsonPayload = html.substring(start + 1, end);
        return mapper.readTree(jsonPayload);
    }

    private static List<URI> buildCandidateUris(int revisionId, int partId) {
        List<URI> uris = new ArrayList<>();
        for (String host : PART_CDN_HOSTS) {
            String url = String.format(Locale.ROOT, "https://%s.cloudfront.net/part/%d/%d", host, revisionId, partId);
            uris.add(URI.create(url));
        }
        return uris;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public record SongSearchResult(int songId, String artist, String title, List<TrackSummary> tracks) {}

    public record TrackSummary(int index, String hash, String name, String instrument, int difficulty, List<Integer> tuning) {}

    public record SongDetails(int songId, int revisionId, Map<String, TrackMeta> tracks) {
        public TrackMeta track(String hash) {
            return tracks.get(hash);
        }
    }

    public record TrackMeta(int partId, int index) {}

    public record SavedTabDto(
            String tabId,
            TabSelection selection,
            SongRequest song,
            String rawContent,
            Instant fetchedAt,
            Instant savedAt,
            Path location) {}

    public static final class StoredTab {
        public String tabId;
        public TabSelection selection;
        public SongRequest song;
        public String rawContent;
        public Instant fetchedAt;
        public Instant savedAt;
    }
}
