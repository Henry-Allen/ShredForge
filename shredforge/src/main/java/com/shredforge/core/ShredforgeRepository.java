package com.shredforge.core;

import com.shredforge.core.model.CalibrationInput;
import com.shredforge.calibration.SimpleCalibrationService;
import com.shredforge.calibration.SimpleSignalProcessor;
import com.shredforge.calibration.TuningLibrary;
import com.shredforge.calibration.TuningPreset;
import com.shredforge.core.model.CalibrationProfile;
import com.shredforge.core.model.FormattedTab;
import com.shredforge.core.model.SessionRequest;
import com.shredforge.core.model.SessionResult;
import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.core.ports.CalibrationService;
import com.shredforge.core.ports.SessionScoringService;
import com.shredforge.tabs.TabManager;
import com.shredforge.tabview.TabRenderingService;
import com.shredforge.tabs.model.SongSelection;
import com.shredforge.tabs.model.TabSearchRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Primary entry point for UI/controllers. Wraps {@link ShredforgeFacade} and exposes high-level operations the UI can
 * call without needing to understand the underlying subsystems.
 */
public final class ShredforgeRepository {

    private final ShredforgeFacade facade;
    private final TabManager tabManager;

    private ShredforgeRepository(Builder builder) {
        this.tabManager = builder.tabManager != null ? builder.tabManager : TabManager.createDefault();
        this.facade = builder.facade != null ? builder.facade : defaultFacade(tabManager);
    }

    public static Builder builder() {
        return new Builder();
    }

    public ShredforgeFacade.RepositoryState describeState() {
        return facade.describeState();
    }

    /**
     * Searches for songs matching the given term.
     * Returns song-level results (each containing all tracks).
     */
    public List<SongSelection> searchSongs(String term) {
        return tabManager.searchSongs(new TabSearchRequest(term));
    }

    public FormattedTab formatTab(TabData tabData) {
        Objects.requireNonNull(tabData, "tabData");
        return facade.formatTab(tabData);
    }

    public TabManager tabManager() {
        return tabManager;
    }

    public CalibrationProfile calibrate(CalibrationInput input) {
        return facade.calibrate(input);
    }

    public List<TuningPreset> availableTunings() {
        return TuningLibrary.commonTunings();
    }

    /**
     * Downloads a GP file for the given song selection.
     * Returns a CompletableFuture that resolves to TabData with the GP file path.
     */
    public CompletableFuture<TabData> downloadGpFile(SongSelection selection) {
        return tabManager.downloadOrGetCached(selection);
    }

    /**
     * Runs a canned happy-path flow that exercises every subsystem. Used by the temporary testing UI.
     */
    public DemoSessionSummary runDemoSession() {
        TabData tabData = fallbackTabData();
        SongRequest songRequest = tabData.song();
        FormattedTab formatted = facade.formatTab(tabData);
        CalibrationInput calibrationInput =
                new CalibrationInput("demo-user", 32.0, Map.of("E2", 82.41, "E4", 329.63));
        CalibrationProfile profile = facade.calibrate(calibrationInput);
        SessionResult sessionResult = facade.runSession("demo-user", tabData, profile);
        return new DemoSessionSummary(songRequest.displayName(), formatted, sessionResult);
    }

    private TabData fallbackTabData() {
        SongRequest songRequest = new SongRequest("Demo Groove", "Shredforge AI", "Standard", "Intermediate");
        return new TabData(
                "demo-source",
                songRequest,
                """
                        E|----------------|
                        B|----------------|
                        G|----------------|
                        D|-----5--7--5----|
                        A|--5-----------7-|
                        E|----------------|
                        """,
                Instant.now(),
                null);
    }

    private static ShredforgeFacade defaultFacade(TabManager tabManager) {
        return ShredforgeFacade.builder()
                .withTabGateway(tabManager)
                .withTabFormatter(new TabRenderingService())
                .withCalibrationService(new SimpleCalibrationService(new SimpleSignalProcessor()))
                .withSessionScoringService(new MockSessionScoringService())
                .build();
    }

    public static final class Builder {
        private ShredforgeFacade facade;
        private TabManager tabManager;

        public Builder withFacade(ShredforgeFacade facade) {
            this.facade = Objects.requireNonNull(facade, "facade");
            return this;
        }

        public Builder withTabManager(TabManager tabManager) {
            this.tabManager = Objects.requireNonNull(tabManager, "tabManager");
            return this;
        }

        public ShredforgeRepository build() {
            return new ShredforgeRepository(this);
        }
    }

    public record DemoSessionSummary(String songName, FormattedTab tab, SessionResult result) {

        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Song: ").append(songName).append(System.lineSeparator());
            sb.append("Accuracy: ").append(String.format("%.1f%%", result.accuracyPercent()))
                    .append(System.lineSeparator());
            if (!result.insights().isEmpty()) {
                sb.append("Insights:").append(System.lineSeparator());
                result.insights().forEach(insight -> sb.append(" - ").append(insight).append(System.lineSeparator()));
            }
            if (tab != null && !tab.svgFragments().isEmpty()) {
                sb.append("SVG Preview:").append(System.lineSeparator());
                tab.svgFragments().forEach(line -> sb.append("   ").append(line).append(System.lineSeparator()));
            }
            return sb.toString();
        }
    }

    private static final class MockSessionScoringService implements SessionScoringService {

        @Override
        public SessionResult score(SessionRequest request) {
            double base = 70 + ThreadLocalRandom.current().nextDouble(25);
            List<String> insights =
                    List.of("Timing was solid!", "Watch bend accuracy on the D string.", "Try increasing tempo by +5 BPM.");
            return new SessionResult(
                    "session-" + request.startedAt().toEpochMilli(),
                    base,
                    insights,
                    Map.of("E", base - 5, "A", base + 2, "D", base + 4),
                    Instant.now());
        }
    }
}
