package com.shredforge;

import com.shredforge.core.ShredforgeFacade;
import com.shredforge.core.model.FormattedTab;
import com.shredforge.core.model.TabData;
import com.shredforge.scoring.TabNoteParser;
import com.shredforge.scoring.model.TabNote;
import com.shredforge.tabview.TabPlaybackController;
import com.shredforge.tabview.render.SongsterrTabFormatter;
import com.shredforge.tabs.model.SavedTabSummary;
import com.shredforge.tabs.model.TabSearchResult;
import com.shredforge.tabs.model.TabSelection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;

public class PrimaryController {
    private final TabPlaybackController playbackController = new TabPlaybackController();
    private final ObservableList<TabListItem> tabOptions = FXCollections.observableArrayList();
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "shredforge-ui-worker");
        t.setDaemon(true);
        return t;
    });
    private final TabNoteParser tabNoteParser = new TabNoteParser();
    private final DecimalFormat tempoFormat = new DecimalFormat("0.0x");
    private final DecimalFormat percentFormat = new DecimalFormat("0.0");

    @FXML
    private Label repositoryStatusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchOnlineButton;

    @FXML
    private Button searchSavedButton;

    @FXML
    private ListView<TabListItem> resultsList;

    @FXML
    private Label searchStatusLabel;

    @FXML
    private WebView tabPreview;

    @FXML
    private Pane playbackOverlay;

    @FXML
    private Rectangle playbackIndicator;

    @FXML
    private Button playPauseButton;

    @FXML
    private Slider tempoSlider;

    @FXML
    private Label tempoLabel;

    @FXML
    private ProgressBar playbackProgressBar;

    @FXML
    private Label scoreStatsLabel;

    private AnimationTimer playbackTimer;
    private boolean playing;
    private double tempoMultiplier = 1.0;
    private double playbackCursorMs;
    private double playbackDurationMs;
    private double playbackBaseMs;
    private long playbackStartNano;

    private TabData currentTabData;
    private FormattedTab currentFormattedTab;
    private List<TabNote> currentNotes = List.of();
    private TabListItem lastRequestedItem;

    @FXML
    public void initialize() {
        refreshStatus();
        configureResultsList();
        configureTempoSlider();
        configurePlaybackOverlay();
        initPlaybackTimer();
        tabPreview.getEngine().loadContent(
                SongsterrTabFormatter.renderMessage("Search online or browse saved tabs to begin."),
                "text/html");
        updatePlaybackUI();
    }

    @FXML
    private void onSearchOnline() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            searchStatusLabel.setText("Enter a term to search online.");
            return;
        }
        runAsync(
                () -> App.repository().searchTabs(term.trim()),
                results -> {
                    if (results.isEmpty()) {
                        searchStatusLabel.setText("No results found online.");
                        tabOptions.clear();
                        return;
                    }
                    tabOptions.setAll(results.stream()
                            .map(TabListItem::fromSearchResult)
                            .toList());
                    resultsList.getSelectionModel().clearSelection();
                    searchStatusLabel.setText("Online search returned " + results.size() + " result(s).");
                },
                error -> searchStatusLabel.setText("Online search failed: " + error.getMessage()));
    }

    @FXML
    private void onSearchSaved() {
        String term = searchField.getText();
        runAsync(
                App.repository()::listSavedTabs,
                saved -> {
                    var filtered = saved.stream()
                            .filter(item -> term == null
                                    || term.isBlank()
                                    || item.selection().displayLabel().toLowerCase().contains(term.toLowerCase()))
                            .map(TabListItem::fromSavedSummary)
                            .toList();
                    tabOptions.setAll(filtered);
                    resultsList.getSelectionModel().clearSelection();
                    searchStatusLabel.setText(
                            filtered.isEmpty() ? "No saved tabs match your search." : "Showing saved tabs.");
                },
                error -> searchStatusLabel.setText("Failed to load saved tabs: " + error.getMessage()));
    }

    @FXML
    private void onPlayPause() {
        if (currentFormattedTab == null) {
            searchStatusLabel.setText("Load a tab before starting playback.");
            return;
        }
        playing = !playing;
        if (playing) {
            playbackStartNano = System.nanoTime();
            playbackBaseMs = playbackCursorMs;
            playPauseButton.setText("Pause");
            playbackTimer.start();
        } else {
            playbackTimer.stop();
            playbackCursorMs = currentCursor();
            playPauseButton.setText("Play");
            updatePlaybackUI();
        }
    }

    @FXML
    private void onRestartPlayback() {
        playbackCursorMs = 0;
        playbackBaseMs = 0;
        playbackStartNano = System.nanoTime();
        if (!playing) {
            updatePlaybackUI();
        }
    }

    @FXML
    private void onOpenCalibration() {
        App.showCalibration();
    }

    @FXML
    private void listSavedTabs() {
        runAsync(
                App.repository()::listSavedTabs,
                saved -> {
                    if (saved.isEmpty()) {
                        searchStatusLabel.setText("No saved tabs yet.");
                        return;
                    }
                    tabOptions.setAll(saved.stream().map(TabListItem::fromSavedSummary).toList());
                    resultsList.getSelectionModel().clearSelection();
                    searchStatusLabel.setText("Showing all saved tabs.");
                },
                error -> searchStatusLabel.setText("Failed to read saved tabs: " + error.getMessage()));
    }

    @FXML
    private void refreshStatus() {
        var state = App.repository().describeState();
        repositoryStatusLabel.setText(describeState(state));
    }

    private void configureResultsList() {
        resultsList.setItems(tabOptions);
        resultsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(TabListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.displayLabel());
                }
            }
        });
        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                requestTabLoad(newItem, false);
            }
        });
        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() > 1) {
                TabListItem selected = resultsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    requestTabLoad(selected, true);
                }
            }
        });
    }

    private void configureTempoSlider() {
        tempoSlider.setMin(0.5);
        tempoSlider.setMax(1.5);
        tempoSlider.setValue(1.0);
        tempoSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tempoMultiplier = newVal.doubleValue();
            tempoLabel.setText("Tempo " + tempoFormat.format(tempoMultiplier));
            if (playing) {
                playbackCursorMs = currentCursor();
                playbackBaseMs = playbackCursorMs;
                playbackStartNano = System.nanoTime();
            }
            updatePlaybackUI();
        });
        tempoLabel.setText("Tempo 1.0x");
    }

    private void configurePlaybackOverlay() {
        playbackOverlay.prefWidthProperty().bind(tabPreview.widthProperty());
        playbackOverlay.prefHeightProperty().bind(tabPreview.heightProperty());
        playbackIndicator.setFill(Color.valueOf("#ffca6f"));
        playbackIndicator.setWidth(3);
        playbackIndicator.setHeight(1000);
        playbackIndicator.setVisible(false);
        playbackOverlay.heightProperty().addListener((obs, oldVal, newVal) -> {
            playbackIndicator.setHeight(newVal.doubleValue());
        });
    }

    private void initPlaybackTimer() {
        playbackTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!playing || playbackDurationMs <= 0) {
                    return;
                }
                double elapsedMs = ((now - playbackStartNano) / 1_000_000.0) * tempoMultiplier;
                playbackCursorMs = playbackBaseMs + elapsedMs;
                if (playbackCursorMs >= playbackDurationMs) {
                    playbackCursorMs = playbackDurationMs;
                    playing = false;
                    stop();
                    playPauseButton.setText("Play");
                }
                updatePlaybackUI();
            }
        };
    }

    private void requestTabLoad(TabListItem item, boolean force) {
        if (!force && Objects.equals(item, lastRequestedItem)) {
            return;
        }
        lastRequestedItem = item;
        loadTabForItem(item);
    }

    private void loadTabForItem(TabListItem item) {
        if (item.selection() != null) {
            searchStatusLabel.setText("Downloading tab from Songsterr...");
            runAsync(
                    () -> App.repository().downloadSelection(item.selection(), true),
                    this::applyTabData,
                    error -> searchStatusLabel.setText("Failed to download tab: " + error.getMessage()));
        } else if (item.savedSummary() != null) {
            searchStatusLabel.setText("Loading saved tab...");
            runAsync(
                    () -> App.repository().loadSavedTab(item.savedSummary().tabId()),
                    this::applyTabData,
                    error -> searchStatusLabel.setText("Failed to load saved tab: " + error.getMessage()));
        }
    }

    private void applyTabData(TabData tabData) {
        if (tabData == null || tabData.rawContent() == null || tabData.rawContent().isBlank()) {
            searchStatusLabel.setText("Tab download returned empty content. Try another track.");
            return;
        }
        try {
            this.currentTabData = tabData;
            this.currentFormattedTab = App.repository().formatTab(tabData);
            playbackController.load(currentFormattedTab);
            tabPreview.getEngine().loadContent(currentFormattedTab.documentHtml(), "text/html");
            this.currentNotes = tabNoteParser.parse(tabData);
            this.playbackDurationMs = currentNotes.isEmpty()
                    ? 0
                    : currentNotes.get(currentNotes.size() - 1).timestampMillis() + 2000;
            this.playbackCursorMs = 0;
            this.playbackBaseMs = 0;
            this.playing = false;
            playbackTimer.stop();
            playPauseButton.setText("Play");
            updatePlaybackUI();
            searchStatusLabel.setText("Loaded " + tabData.song().displayName());
        } catch (Exception ex) {
            searchStatusLabel.setText("Failed to render tab: " + ex.getMessage());
            ex.printStackTrace();
            System.err.println("Tab JSON snippet:\n" + tabData.rawContent().substring(0, Math.min(500, tabData.rawContent().length())));
        }
    }

    private void updatePlaybackUI() {
        double fraction = playbackDurationMs <= 0 ? 0 : Math.min(1.0, playbackCursorMs / playbackDurationMs);
        playbackProgressBar.setProgress(fraction);
        updateIndicatorPosition(fraction);
        updateScoreStats();
    }

    private void updateIndicatorPosition(double fraction) {
        if (playbackOverlay.getWidth() <= 0) {
            playbackIndicator.setVisible(false);
            return;
        }
        playbackIndicator.setVisible(true);
        double x = playbackOverlay.getWidth() * fraction - playbackIndicator.getWidth() / 2;
        playbackIndicator.setLayoutX(Math.max(0, x));
    }

    private void updateScoreStats() {
        if (currentNotes.isEmpty()) {
            scoreStatsLabel.setText("Accuracy: -- | Hits: 0/0");
            return;
        }
        long hits = currentNotes.stream().filter(n -> n.timestampMillis() <= playbackCursorMs).count();
        double accuracy = hits * 100.0 / currentNotes.size();
        scoreStatsLabel.setText(String.format(
                "Accuracy: %s%% | Notes: %d/%d",
                percentFormat.format(accuracy), hits, currentNotes.size()));
    }

    private double currentCursor() {
        double elapsedMs = ((System.nanoTime() - playbackStartNano) / 1_000_000.0) * tempoMultiplier;
        return Math.min(playbackDurationMs, playbackBaseMs + elapsedMs);
    }

    private <T> void runAsync(java.util.concurrent.Callable<T> task, java.util.function.Consumer<T> onSuccess, java.util.function.Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, ioExecutor)
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        onError.accept(throwable.getCause() == null ? throwable : throwable.getCause());
                    } else {
                        onSuccess.accept(result);
                    }
                }));
    }

    private static String describeState(ShredforgeFacade.RepositoryState state) {
        List<String> missing = new ArrayList<>();
        if (!state.tabGatewayConfigured()) {
            missing.add("Tab Gateway");
        }
        if (!state.tabFormatterConfigured()) {
            missing.add("Tab Formatter");
        }
        if (!state.calibrationServiceConfigured()) {
            missing.add("Calibration Service");
        }
        if (!state.sessionScoringServiceConfigured()) {
            missing.add("Session Scoring");
        }
        if (missing.isEmpty()) {
            return "All subsystems ready. Session testing available.";
        }
        return "Missing subsystems: " + String.join(", ", missing);
    }

    private record TabListItem(TabSelection selection, SavedTabSummary savedSummary, boolean online) {

        static TabListItem fromSearchResult(TabSearchResult result) {
            return new TabListItem(result.selection(), null, true);
        }

        static TabListItem fromSavedSummary(SavedTabSummary summary) {
            return new TabListItem(summary.selection(), summary, false);
        }

        String displayLabel() {
            String prefix = online ? "[Online]" : "[Saved]";
            String label = selection != null ? selection.displayLabel() : savedSummary.selection().displayLabel();
            return prefix + " " + label;
        }
    }
}
