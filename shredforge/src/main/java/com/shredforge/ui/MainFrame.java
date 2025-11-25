package com.shredforge.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shredforge.SwingApp;
import com.shredforge.core.ShredforgeRepository;
import com.shredforge.core.model.AudioDeviceInfo;
import com.shredforge.core.model.ExpectedNote;
import com.shredforge.core.model.LiveScoreSnapshot;
import com.shredforge.core.model.PracticeConfig;
import com.shredforge.tabs.model.SongSelection;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDownloadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application frame with Swing toolbar and JCEF browser for AlphaTab.
 */
public class MainFrame extends JFrame {
    
    private final CefApp cefApp;
    private final CefClient cefClient;
    private final CefBrowser browser;
    private final ShredforgeRepository repository;
    private final ExecutorService executor;
    
    private JLabel statusLabel;
    private JTextField searchField;
    private JButton backToAlphaTabBtn;
    private volatile boolean pageLoaded = false;
    private String soundFontDataUrl;
    private String alphaTabUrl;
    private SongSelection pendingDownloadSong;
    private Path pendingGpFileToLoad;
    private final Path gpStorageDir;
    private final Path credentialsFile;
    private String songsterrEmail;
    private String songsterrPassword;
    private boolean loginAttempted = false;
    
    // Practice mode state
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private JButton practiceBtn;
    private JButton audioSelectBtn;
    private JLabel scoreLabel;
    private AudioDeviceInfo selectedAudioDevice = AudioDeviceInfo.systemDefault();
    private volatile boolean practiceMode = false;
    private volatile boolean notesLoaded = false;
    private ScheduledExecutorService positionPoller;
    private volatile String pendingNotesJson = null;
    private volatile double pendingDurationMs = 0;
    
    public MainFrame(CefApp cefApp) {
        super("ShredForge");
        this.cefApp = cefApp;
        this.repository = ShredforgeRepository.builder().build();
        this.gpStorageDir = Paths.get(System.getProperty("user.home"), ".shredforge", "gp-files");
        this.credentialsFile = Paths.get(System.getProperty("user.home"), ".shredforge", "songsterr-credentials");
        this.executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "shredforge-worker");
            t.setDaemon(true);
            return t;
        });
        
        // Ensure GP storage directory exists
        try {
            Files.createDirectories(gpStorageDir);
        } catch (Exception e) {
            System.err.println("Failed to create GP storage directory: " + e.getMessage());
        }
        
        // Load saved Songsterr credentials, prompt if not found
        loadSongsterrCredentials();
        if (songsterrEmail == null || songsterrPassword == null) {
            // Prompt for credentials on first launch
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                    "To download GP files from Songsterr, you need to log in.\n" +
                    "Please enter your Songsterr credentials.",
                    "Songsterr Login Required", JOptionPane.INFORMATION_MESSAGE);
                showLoginDialog();
            });
        }
        
        // Load soundfont
        soundFontDataUrl = loadSoundFontAsDataUrl();
        
        // Create CEF client
        cefClient = cefApp.createClient();
        
        // Set up message router for JS→Java communication (for note extraction)
        CefMessageRouter.CefMessageRouterConfig routerConfig = new CefMessageRouter.CefMessageRouterConfig();
        routerConfig.jsQueryFunction = "cefQuery";
        routerConfig.jsCancelFunction = "cefQueryCancel";
        CefMessageRouter messageRouter = CefMessageRouter.create(routerConfig);
        messageRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, 
                    String request, boolean persistent, CefQueryCallback callback) {
                // Handle messages from JavaScript
                if (request.startsWith("notes:")) {
                    // Format: notes:<json>:<durationMs>
                    String data = request.substring(6);
                    int lastColon = data.lastIndexOf(':');
                    if (lastColon > 0) {
                        pendingNotesJson = data.substring(0, lastColon);
                        try {
                            pendingDurationMs = Double.parseDouble(data.substring(lastColon + 1));
                        } catch (NumberFormatException e) {
                            pendingDurationMs = 180000;
                        }
                        System.out.println("Received notes from JS, duration: " + pendingDurationMs + "ms");
                        callback.success("ok");
                        return true;
                    }
                } else if (request.startsWith("trackChanged:")) {
                    // Track changed - re-extract notes if in practice mode
                    String trackIndexStr = request.substring(13);
                    System.out.println("Track changed to: " + trackIndexStr);
                    if (practiceMode) {
                        // Re-extract notes for the new track
                        executor.submit(() -> {
                            System.out.println("Re-extracting notes for new track...");
                            extractAndLoadNotes();
                            int noteCount = pendingNotesJson != null ? 
                                    (pendingNotesJson.equals("[]") ? 0 : pendingNotesJson.split("\\{").length - 1) : 0;
                            System.out.println("Reloaded " + noteCount + " notes for track " + trackIndexStr);
                        });
                    }
                    callback.success("ok");
                    return true;
                } else if (request.startsWith("playbackPosition:")) {
                    // Playback position update from AlphaTab
                    String posStr = request.substring(17);
                    try {
                        double positionMs = Double.parseDouble(posStr);
                        if (practiceMode && repository != null) {
                            repository.updatePlaybackPosition(positionMs);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid position
                    }
                    callback.success("ok");
                    return true;
                }
                callback.failure(0, "Unknown request");
                return true;
            }
        }, true);
        cefClient.addMessageRouter(messageRouter);
        
        // Add download handler to intercept GP file downloads from Songsterr
        cefClient.addDownloadHandler(new CefDownloadHandler() {
            @Override
            public void onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem,
                    String suggestedName, CefBeforeDownloadCallback callback) {
                // Save GP files to our storage directory
                String filename = suggestedName;
                if (pendingDownloadSong != null) {
                    filename = sanitizeFilename(pendingDownloadSong.songId() + "_" + 
                            pendingDownloadSong.artist() + "_" + pendingDownloadSong.title()) + ".gp";
                }
                Path targetPath = gpStorageDir.resolve(filename);
                System.out.println("Downloading GP file to: " + targetPath);
                callback.Continue(targetPath.toString(), false);
            }

            @Override
            public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem,
                    CefDownloadItemCallback callback) {
                if (downloadItem.isComplete()) {
                    Path downloadedFile = Paths.get(downloadItem.getFullPath());
                    System.out.println("Download complete: " + downloadedFile);
                    
                    // Store the file to load after AlphaTab page loads
                    pendingGpFileToLoad = downloadedFile;
                    
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Loading: " + downloadedFile.getFileName());
                        // Navigate to AlphaTab - the GP file will be loaded when page finishes loading
                        navigateToAlphaTab();
                    });
                } else if (downloadItem.isCanceled()) {
                    SwingUtilities.invokeLater(() -> 
                        statusLabel.setText("Download canceled"));
                } else if (downloadItem.isInProgress()) {
                    int percent = downloadItem.getPercentComplete();
                    SwingUtilities.invokeLater(() -> 
                        statusLabel.setText("Downloading... " + percent + "%"));
                }
            }
        });
        
        // Add load handler
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    pageLoaded = true;
                    String url = browser.getURL();
                    
                    // Check if we're on Songsterr and need to handle login
                    if (url != null && url.contains("songsterr.com")) {
                        handleSongsterrPage(browser, url);
                    } else {
                        // We're on AlphaTab page
                        // Inject soundfont URL for AlphaTab
                        if (soundFontDataUrl != null && !soundFontDataUrl.isEmpty()) {
                            String js = "window.javaSoundFontUrl = '" + escapeJs(soundFontDataUrl) + "';";
                            browser.executeJavaScript(js, "", 0);
                            System.out.println("Injected soundfont data URL");
                        }
                        
                        // Check if there's a pending GP file to load
                        if (pendingGpFileToLoad != null) {
                            Path gpFile = pendingGpFileToLoad;
                            pendingGpFileToLoad = null; // Clear it
                            // Load after a short delay to ensure AlphaTab is ready
                            executor.submit(() -> {
                                try {
                                    Thread.sleep(500);
                                    loadGpFileIntoAlphaTab(gpFile);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> statusLabel.setText("Ready"));
                        }
                    }
                }
            }
        });
        
        // Get HTML URL
        URL htmlUrl = MainFrame.class.getResource("/com/shredforge/alphatab/index.html");
        if (htmlUrl == null) {
            throw new IllegalStateException("alphatab/index.html not found on classpath");
        }
        alphaTabUrl = htmlUrl.toExternalForm();
        
        // Create browser
        browser = cefClient.createBrowser(alphaTabUrl, false, false);
        
        // Setup UI
        setupUI();
        
        // Window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                browser.close(true);
                cefClient.dispose();
                SwingApp.shutdown();
                System.exit(0);
            }
        });
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        JButton searchButton = new JButton("Search Tabs");
        searchButton.addActionListener(e -> showSearchDialog());
        toolbar.add(searchButton);
        
        JButton myTabsButton = new JButton("My Tabs");
        myTabsButton.addActionListener(e -> showMyTabs());
        toolbar.add(myTabsButton);
        
        toolbar.addSeparator();
        
        // Back to AlphaTab button (hidden by default, shown when on Songsterr)
        backToAlphaTabBtn = new JButton("← Back to AlphaTab");
        backToAlphaTabBtn.setVisible(false);
        backToAlphaTabBtn.addActionListener(e -> navigateToAlphaTab());
        toolbar.add(backToAlphaTabBtn);
        
        toolbar.addSeparator();
        
        // Practice mode controls - use a button instead of combo (JCEF conflicts with popups)
        audioSelectBtn = new JButton("Audio: System Default");
        audioSelectBtn.addActionListener(e -> showAudioDeviceSelector());
        toolbar.add(audioSelectBtn);
        
        toolbar.addSeparator();
        
        practiceBtn = new JButton("Practice");
        practiceBtn.addActionListener(e -> togglePracticeMode());
        toolbar.add(practiceBtn);
        
        scoreLabel = new JLabel("Score: --");
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        toolbar.add(scoreLabel);
        
        toolbar.addSeparator();
        
        statusLabel = new JLabel("Loading...");
        toolbar.add(statusLabel);
        
        add(toolbar, BorderLayout.NORTH);
        
        // Browser panel
        Component browserUI = browser.getUIComponent();
        add(browserUI, BorderLayout.CENTER);
    }
    
    /**
     * Shows a dialog to select the audio input device.
     */
    private void showAudioDeviceSelector() {
        // Get devices in background to avoid blocking
        executor.submit(() -> {
            try {
                List<AudioDeviceInfo> devices = repository.listAudioDevices();
                SwingUtilities.invokeLater(() -> {
                    if (devices.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "No audio input devices found.",
                                "Audio Devices", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    // Create a simple selection dialog
                    AudioDeviceInfo[] deviceArray = devices.toArray(new AudioDeviceInfo[0]);
                    AudioDeviceInfo selected = (AudioDeviceInfo) JOptionPane.showInputDialog(
                            this,
                            "Select audio input device:",
                            "Audio Device",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            deviceArray,
                            selectedAudioDevice
                    );
                    
                    if (selected != null) {
                        selectedAudioDevice = selected;
                        audioSelectBtn.setText("Audio: " + truncateDeviceName(selected.name(), 20));
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to enumerate audio devices: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Failed to list audio devices: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    
    private String truncateDeviceName(String name, int maxLen) {
        if (name == null) return "Unknown";
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 3) + "...";
    }
    
    private void showSearchDialog() {
        JDialog dialog = new JDialog(this, "Search Songs", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        searchField = new JTextField();
        JButton searchBtn = new JButton("Search");
        
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        
        // Results list - now shows songs instead of individual tracks
        DefaultListModel<SongSelection> listModel = new DefaultListModel<>();
        JList<SongSelection> resultsList = new JList<>(listModel);
        resultsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SongSelection song) {
                    setText(song.displayLabel());
                }
                return this;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(resultsList);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loadBtn = new JButton("Download & Load");
        JButton cancelBtn = new JButton("Cancel");
        
        loadBtn.setEnabled(false);
        buttonPanel.add(loadBtn);
        buttonPanel.add(cancelBtn);
        
        // Wire up actions
        searchBtn.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                statusLabel.setText("Searching...");
                executor.submit(() -> {
                    try {
                        List<SongSelection> results = repository.searchSongs(query);
                        SwingUtilities.invokeLater(() -> {
                            listModel.clear();
                            results.forEach(listModel::addElement);
                            statusLabel.setText("Found " + results.size() + " songs");
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Search failed");
                            JOptionPane.showMessageDialog(dialog, 
                                "Search failed: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                });
            }
        });
        
        resultsList.addListSelectionListener(e -> {
            loadBtn.setEnabled(resultsList.getSelectedValue() != null);
        });
        
        loadBtn.addActionListener(e -> {
            SongSelection selected = resultsList.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                loadSong(selected);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        // Enter key triggers search
        searchField.addActionListener(e -> searchBtn.doClick());
        
        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    /**
     * Opens the song on Songsterr so the user can manually click download.
     * The download will be intercepted by our CefDownloadHandler.
     */
    private void loadSong(SongSelection song) {
        pendingDownloadSong = song;
        String songUrl = buildSongsterrUrl(song);
        statusLabel.setText("Opening Songsterr - click the download button to get the GP file");
        backToAlphaTabBtn.setVisible(true);
        browser.loadURL(songUrl);
    }
    
    /**
     * Navigates back to the AlphaTab page.
     */
    private void navigateToAlphaTab() {
        backToAlphaTabBtn.setVisible(false);
        browser.loadURL(alphaTabUrl);
    }
    
    /**
     * Builds the Songsterr URL for a song.
     */
    private String buildSongsterrUrl(SongSelection song) {
        String artistSlug = slugify(song.artist());
        String titleSlug = slugify(song.title());
        return "https://www.songsterr.com/a/wsa/" + artistSlug + "-" + titleSlug + "-tab-s" + song.songId();
    }
    
    /**
     * Converts a string to a URL-friendly slug.
     */
    private static String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
    
    /**
     * Sanitizes a filename by removing invalid characters.
     */
    private static String sanitizeFilename(String input) {
        return input.replaceAll("[^a-zA-Z0-9-_]", "_").replaceAll("_+", "_");
    }
    
    /**
     * Loads a Guitar Pro file into AlphaTab by reading it and sending as base64.
     */
    private void loadGpFileIntoAlphaTab(Path gpFilePath) {
        try {
            byte[] bytes = Files.readAllBytes(gpFilePath);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String script = String.format(
                "window.loadGpFromJava && window.loadGpFromJava('%s')",
                base64.replace("'", "\\'"));
            browser.executeJavaScript(script, "", 0);
            System.out.println("Loaded GP file: " + gpFilePath);
        } catch (Exception e) {
            System.err.println("Failed to load GP file: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Failed to load GP file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showMyTabs() {
        JDialog dialog = new JDialog(this, "My Downloaded Tabs", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        // List of downloaded GP files
        DefaultListModel<Path> listModel = new DefaultListModel<>();
        JList<Path> fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Path path) {
                    String filename = path.getFileName().toString();
                    // Parse filename format: songId_Artist_Title.gp
                    String displayName = parseGpFilename(filename);
                    setText(displayName);
                }
                return this;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loadBtn = new JButton("Load");
        loadBtn.setEnabled(false);
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setEnabled(false);
        JButton cancelBtn = new JButton("Cancel");
        
        buttonPanel.add(deleteBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load GP files from storage directory
        try {
            if (Files.exists(gpStorageDir)) {
                Files.list(gpStorageDir)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".gp") || name.endsWith(".gp3") || 
                               name.endsWith(".gp4") || name.endsWith(".gp5") || name.endsWith(".gpx");
                    })
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .forEach(listModel::addElement);
            }
        } catch (Exception e) {
            System.err.println("Failed to list GP files: " + e.getMessage());
        }
        
        // Selection listener
        fileList.addListSelectionListener(e -> {
            boolean hasSelection = fileList.getSelectedValue() != null;
            loadBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
        });
        
        // Load button
        loadBtn.addActionListener(e -> {
            Path selected = fileList.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                pendingGpFileToLoad = selected;
                navigateToAlphaTab();
            }
        });
        
        // Delete button
        deleteBtn.addActionListener(e -> {
            Path selected = fileList.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + selected.getFileName() + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        Files.deleteIfExists(selected);
                        listModel.removeElement(selected);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog,
                            "Failed to delete: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        // Cancel button
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        // Double-click to load
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Path selected = fileList.getSelectedValue();
                    if (selected != null) {
                        dialog.dispose();
                        pendingGpFileToLoad = selected;
                        navigateToAlphaTab();
                    }
                }
            }
        });
        
        dialog.setVisible(true);
    }
    
    /**
     * Parses a GP filename into a display-friendly format.
     * Expected format: songId_Artist_Title.gp
     */
    private String parseGpFilename(String filename) {
        // Remove extension
        String name = filename;
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            name = name.substring(0, dotIdx);
        }
        
        // Try to parse songId_Artist_Title format
        int firstUnderscore = name.indexOf('_');
        if (firstUnderscore > 0) {
            String rest = name.substring(firstUnderscore + 1);
            // Replace underscores with spaces for display
            rest = rest.replace('_', ' ');
            // Try to find artist/title split (second major underscore group)
            // Format is typically: Artist_Name_Song_Title -> "Artist Name - Song Title"
            // This is a best-effort parse
            return rest;
        }
        
        return name.replace('_', ' ');
    }
    
    private void renderScore(String json) {
        String escaped = escapeJs(json);
        String script = "if(typeof renderScore === 'function') { renderScore('" + escaped + "'); }";
        browser.executeJavaScript(script, "", 0);
    }
    
    private String loadSoundFontAsDataUrl() {
        try (var is = MainFrame.class.getResourceAsStream("/com/shredforge/alphatab/sonivox.sf2")) {
            if (is == null) {
                System.err.println("SoundFont not found in resources");
                return null;
            }
            byte[] bytes = is.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            System.out.println("Loaded soundfont: " + bytes.length + " bytes");
            return "data:audio/sf2;base64," + base64;
        } catch (Exception e) {
            System.err.println("Failed to load soundfont: " + e.getMessage());
            return null;
        }
    }
    
    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    // --- Songsterr Login Management ---
    
    /**
     * Loads saved Songsterr credentials from disk.
     */
    private void loadSongsterrCredentials() {
        try {
            if (Files.exists(credentialsFile)) {
                java.util.List<String> lines = Files.readAllLines(credentialsFile);
                if (lines.size() >= 2) {
                    songsterrEmail = new String(Base64.getDecoder().decode(lines.get(0)));
                    songsterrPassword = new String(Base64.getDecoder().decode(lines.get(1)));
                    System.out.println("Loaded Songsterr credentials for: " + songsterrEmail);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load Songsterr credentials: " + e.getMessage());
        }
    }
    
    /**
     * Saves Songsterr credentials to disk (base64 encoded for basic obfuscation).
     */
    private void saveSongsterrCredentials() {
        try {
            Files.createDirectories(credentialsFile.getParent());
            String encoded = Base64.getEncoder().encodeToString(songsterrEmail.getBytes()) + "\n" +
                           Base64.getEncoder().encodeToString(songsterrPassword.getBytes());
            Files.writeString(credentialsFile, encoded);
            System.out.println("Saved Songsterr credentials");
        } catch (Exception e) {
            System.err.println("Failed to save Songsterr credentials: " + e.getMessage());
        }
    }
    
    /**
     * Shows a dialog to enter Songsterr credentials.
     */
    private boolean showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField emailField = new JTextField(songsterrEmail != null ? songsterrEmail : "");
        JPasswordField passwordField = new JPasswordField(songsterrPassword != null ? songsterrPassword : "");
        JCheckBox rememberBox = new JCheckBox("Remember credentials", true);
        
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel(""));
        panel.add(rememberBox);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
                "Songsterr Login Required", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            songsterrEmail = emailField.getText().trim();
            songsterrPassword = new String(passwordField.getPassword());
            
            if (!songsterrEmail.isEmpty() && !songsterrPassword.isEmpty()) {
                if (rememberBox.isSelected()) {
                    saveSongsterrCredentials();
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles page loads on Songsterr - detects login page and auto-fills credentials.
     */
    private void handleSongsterrPage(CefBrowser browser, String url) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("On Songsterr - click download button for GP file");
            backToAlphaTabBtn.setVisible(true);
        });
        
        // Execute after a short delay to let the page render
        executor.submit(() -> {
            try {
                Thread.sleep(1500);
                
                // If we have credentials and haven't tried logging in yet, attempt auto-login
                if (songsterrEmail != null && songsterrPassword != null && !loginAttempted) {
                    attemptAutoLogin(browser);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Attempts to auto-fill and submit the Songsterr login form.
     * First clicks the SIGN IN button to open the modal, then fills credentials.
     */
    private void attemptAutoLogin(CefBrowser browser) {
        if (songsterrEmail == null || songsterrPassword == null) {
            // Prompt for credentials
            SwingUtilities.invokeLater(() -> {
                if (showLoginDialog()) {
                    attemptAutoLogin(browser);
                }
            });
            return;
        }
        
        loginAttempted = true;
        
        // Step 1: Click the SIGN IN button to open the login modal
        String clickSignInJs = """
            (function() {
                console.log('ShredForge: Looking for SIGN IN button...');
                
                // Look for the SIGN IN link/button in the header
                var signInLinks = document.querySelectorAll('a, button');
                for (var i = 0; i < signInLinks.length; i++) {
                    var el = signInLinks[i];
                    var text = el.textContent || el.innerText || '';
                    if (text.trim().toUpperCase() === 'SIGN IN') {
                        console.log('ShredForge: Found SIGN IN button, clicking...');
                        el.click();
                        return 'clicked_signin';
                    }
                }
                
                // Also try looking for elements with href containing 'sign' or 'login'
                var loginLink = document.querySelector('a[href*="sign"], a[href*="login"]');
                if (loginLink) {
                    console.log('ShredForge: Found login link, clicking...');
                    loginLink.click();
                    return 'clicked_login_link';
                }
                
                return 'no_signin_button';
            })();
            """;
        
        browser.executeJavaScript(clickSignInJs, "", 0);
        System.out.println("Clicked SIGN IN button");
        
        // Step 2: Wait for modal to appear, then fill credentials
        executor.submit(() -> {
            try {
                Thread.sleep(1500); // Wait for modal to open
                fillLoginCredentials(browser);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        SwingUtilities.invokeLater(() -> 
            statusLabel.setText("Opening login form..."));
    }
    
    /**
     * Fills the login form credentials after the modal is open.
     */
    private void fillLoginCredentials(CefBrowser browser) {
        String loginJs = String.format("""
            (function() {
                console.log('ShredForge: Looking for login form fields...');
                
                // Find email input - look for placeholder text "Email"
                var emailInput = document.querySelector('input[placeholder="Email"], input[type="email"], input[name="email"]');
                var passwordInput = document.querySelector('input[placeholder="Password"], input[type="password"]');
                
                if (emailInput && passwordInput) {
                    console.log('ShredForge: Found login form fields');
                    
                    // Fill in credentials using both value and simulated typing
                    emailInput.focus();
                    emailInput.value = '%s';
                    emailInput.dispatchEvent(new Event('input', { bubbles: true }));
                    emailInput.dispatchEvent(new Event('change', { bubbles: true }));
                    
                    passwordInput.focus();
                    passwordInput.value = '%s';
                    passwordInput.dispatchEvent(new Event('input', { bubbles: true }));
                    passwordInput.dispatchEvent(new Event('change', { bubbles: true }));
                    
                    console.log('ShredForge: Credentials filled');
                    
                    // Find the "Sign in" submit button (green button)
                    setTimeout(function() {
                        var buttons = document.querySelectorAll('button');
                        for (var i = 0; i < buttons.length; i++) {
                            var btn = buttons[i];
                            var text = btn.textContent || btn.innerText || '';
                            if (text.trim().toLowerCase() === 'sign in') {
                                console.log('ShredForge: Found Sign in button, clicking...');
                                btn.click();
                                return;
                            }
                        }
                        // Fallback: try submit button
                        var submitBtn = document.querySelector('button[type="submit"]');
                        if (submitBtn) {
                            submitBtn.click();
                        }
                    }, 500);
                    
                    return 'credentials_filled';
                }
                
                console.log('ShredForge: Login form not found');
                return 'no_login_form';
            })();
            """, escapeJs(songsterrEmail), escapeJs(songsterrPassword));
        
        browser.executeJavaScript(loginJs, "", 0);
        System.out.println("Filled login credentials for: " + songsterrEmail);
        
        SwingUtilities.invokeLater(() -> 
            statusLabel.setText("Logging in to Songsterr..."));
    }
    
    // ==================== Practice Mode Methods ====================
    
    /**
     * Toggles practice mode on/off.
     */
    private void togglePracticeMode() {
        if (practiceMode) {
            stopPracticeMode();
        } else {
            startPracticeMode();
        }
    }
    
    /**
     * Starts practice mode - extracts notes from AlphaTab and begins scoring.
     */
    private void startPracticeMode() {
        // First, extract notes from AlphaTab
        executor.submit(() -> {
            try {
                // Get notes from JavaScript
                extractAndLoadNotes();
                
                // Wait a bit for notes to be extracted
                Thread.sleep(500);
                
                if (!notesLoaded) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("No notes loaded - load a tab first");
                        JOptionPane.showMessageDialog(this,
                                "Please load a Guitar Pro file first before starting practice mode.",
                                "No Tab Loaded", JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                // Get selected audio device
                AudioDeviceInfo selectedDevice = selectedAudioDevice;
                if (selectedDevice == null) {
                    selectedDevice = AudioDeviceInfo.systemDefault();
                }
                
                // Create practice config
                PracticeConfig config = repository.defaultPracticeConfig()
                        .withAudioDevice(selectedDevice);
                
                // Set up note result listener for visual feedback
                repository.setNoteResultListener(new com.shredforge.scoring.LivePracticeScoringService.NoteResultListener() {
                    @Override
                    public void onNoteHit(com.shredforge.core.model.ExpectedNote note, int noteIndex) {
                        // Note hit - show green feedback
                        SwingUtilities.invokeLater(() -> {
                            String js = String.format("window.markNoteResult('%s', true);",
                                    note.noteName().replace("'", "\\'"));
                            browser.executeJavaScript(js, "", 0);
                        });
                    }
                    
                    @Override
                    public void onNoteMissed(com.shredforge.core.model.ExpectedNote note, int noteIndex) {
                        // Note missed - show red feedback
                        SwingUtilities.invokeLater(() -> {
                            String js = String.format("window.markNoteResult('%s', false);",
                                    note.noteName().replace("'", "\\'"));
                            browser.executeJavaScript(js, "", 0);
                        });
                    }
                });
                
                // Show the feedback panel
                browser.executeJavaScript("window.showPracticeFeedback && window.showPracticeFeedback();", "", 0);
                
                // Start the practice session
                repository.startPracticeSession(config, this::onScoreUpdate);
                
                // Start position polling
                startPositionPolling();
                
                practiceMode = true;
                
                SwingUtilities.invokeLater(() -> {
                    practiceBtn.setText("Stop Practice");
                    statusLabel.setText("Practice mode active - play along!");
                    // Tell AlphaTab to start position updates
                    browser.executeJavaScript("window.startPositionUpdates && window.startPositionUpdates();", "", 0);
                });
                
            } catch (Exception e) {
                System.err.println("Failed to start practice mode: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Failed to start practice mode");
                    JOptionPane.showMessageDialog(this,
                            "Failed to start practice mode: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    
    /**
     * Stops practice mode and shows final score.
     */
    private void stopPracticeMode() {
        practiceMode = false;
        
        // Stop position polling
        stopPositionPolling();
        
        // Tell AlphaTab to stop position updates and clear note highlights
        browser.executeJavaScript("window.stopPositionUpdates && window.stopPositionUpdates(); window.clearNoteHighlights && window.clearNoteHighlights();", "", 0);
        
        // Clear the note result listener
        repository.setNoteResultListener(null);
        
        // Stop the practice session and get final score
        LiveScoreSnapshot finalScore = repository.stopPracticeSession();
        
        SwingUtilities.invokeLater(() -> {
            practiceBtn.setText("Practice");
            updateScoreDisplay(finalScore);
            statusLabel.setText("Practice session ended");
            
            // Show summary dialog
            String message = String.format(
                    "Practice Session Complete!\n\n" +
                    "Overall Accuracy: %.1f%%\n" +
                    "Notes Hit: %d / %d\n" +
                    "Partial Accuracy (played): %.1f%%\n" +
                    "Notes Played: %d / %d",
                    finalScore.overallAccuracyPercent(),
                    finalScore.hitsOverall(),
                    finalScore.totalNotesInSong(),
                    finalScore.partialAccuracyPercent(),
                    finalScore.notesPlayedSoFar(),
                    finalScore.totalNotesInSong()
            );
            JOptionPane.showMessageDialog(this, message, "Practice Results", JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Extracts notes from AlphaTab and loads them into the scoring service.
     */
    private void extractAndLoadNotes() {
        // Reset pending data
        pendingNotesJson = null;
        pendingDurationMs = 0;
        
        // Execute JavaScript to extract notes and send them via cefQuery
        // Note: extractNotesForScoring() with no argument uses the currently selected track
        String extractScript = """
            (function() {
                try {
                    var notesJson = window.extractNotesForScoring ? window.extractNotesForScoring() : '[]';
                    var duration = window.getScoreDurationMs ? window.getScoreDurationMs() : 0;
                    var trackIdx = window.getCurrentTrackIndex ? window.getCurrentTrackIndex() : 0;
                    console.log('ShredForge: Extracted notes for track ' + trackIdx + ', sending to Java...');
                    
                    // Send to Java via cefQuery
                    if (window.cefQuery) {
                        window.cefQuery({
                            request: 'notes:' + notesJson + ':' + duration,
                            onSuccess: function(response) {
                                console.log('ShredForge: Notes sent successfully');
                            },
                            onFailure: function(errorCode, errorMessage) {
                                console.error('ShredForge: Failed to send notes:', errorMessage);
                            }
                        });
                    } else {
                        console.error('ShredForge: cefQuery not available');
                    }
                } catch (e) {
                    console.error('ShredForge: Error extracting notes:', e);
                }
            })();
            """;
        
        browser.executeJavaScript(extractScript, "", 0);
        
        // Wait for the callback to be processed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now parse the notes that were received via the message router
        parseAndLoadNotes();
    }
    
    /**
     * Parses the notes received from JavaScript and loads them into the scoring service.
     */
    private void parseAndLoadNotes() {
        if (pendingNotesJson == null || pendingNotesJson.isEmpty() || pendingNotesJson.equals("[]")) {
            System.out.println("No notes received from JavaScript");
            notesLoaded = false;
            repository.loadExpectedNotes(new ArrayList<>(), pendingDurationMs > 0 ? pendingDurationMs : 180000);
            return;
        }
        
        try {
            // Parse the JSON array of notes
            List<ExpectedNote> notes = new ArrayList<>();
            
            // Use Jackson to parse the JSON
            var notesArray = JSON_MAPPER.readTree(pendingNotesJson);
            
            for (var noteNode : notesArray) {
                double timeMs = noteNode.has("timeMs") ? noteNode.get("timeMs").asDouble() : 0;
                double durationMs = noteNode.has("durationMs") ? noteNode.get("durationMs").asDouble() : 100;
                int midi = noteNode.has("midi") ? noteNode.get("midi").asInt() : 0;
                int string = noteNode.has("string") ? noteNode.get("string").asInt() : 0;
                int fret = noteNode.has("fret") ? noteNode.get("fret").asInt() : 0;
                int measureIndex = noteNode.has("measureIndex") ? noteNode.get("measureIndex").asInt() : 0;
                int beatIndex = noteNode.has("beatIndex") ? noteNode.get("beatIndex").asInt() : 0;
                String noteName = noteNode.has("noteName") ? noteNode.get("noteName").asText() : "";
                
                notes.add(new ExpectedNote(timeMs, durationMs, midi, string, fret, measureIndex, beatIndex, noteName));
            }
            
            System.out.println("Parsed " + notes.size() + " notes from AlphaTab");
            notesLoaded = notes.size() > 0;
            
            // Load into the scoring service
            double duration = pendingDurationMs > 0 ? pendingDurationMs : 180000;
            repository.loadExpectedNotes(notes, duration);
            
        } catch (Exception e) {
            System.err.println("Failed to parse notes JSON: " + e.getMessage());
            e.printStackTrace();
            notesLoaded = false;
            repository.loadExpectedNotes(new ArrayList<>(), 180000);
        }
    }
    
    /**
     * Starts polling AlphaTab for playback position updates.
     */
    private void startPositionPolling() {
        if (positionPoller != null) {
            positionPoller.shutdownNow();
        }
        
        positionPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "position-poller");
            t.setDaemon(true);
            return t;
        });
        
        positionPoller.scheduleAtFixedRate(() -> {
            if (!practiceMode) return;
            
            // Get position from JavaScript
            browser.executeJavaScript(
                    "if (window.javaPositionCallback) { " +
                    "  window.javaPositionCallback(window.getPlaybackPositionMs ? window.getPlaybackPositionMs() : 0); " +
                    "} else { " +
                    "  var pos = window.getPlaybackPositionMs ? window.getPlaybackPositionMs() : 0; " +
                    "  console.log('Position: ' + pos); " +
                    "}",
                    "", 0);
            
            // For now, we'll estimate position based on time elapsed
            // In production, you'd use proper JS-Java communication
            
        }, 0, 50, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops position polling.
     */
    private void stopPositionPolling() {
        if (positionPoller != null) {
            positionPoller.shutdownNow();
            positionPoller = null;
        }
    }
    
    /**
     * Called when the score is updated during practice.
     */
    private void onScoreUpdate(LiveScoreSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> updateScoreDisplay(snapshot));
    }
    
    /**
     * Updates the score display label.
     */
    private void updateScoreDisplay(LiveScoreSnapshot snapshot) {
        if (snapshot == null) {
            scoreLabel.setText("Score: --");
            return;
        }
        
        String scoreText = String.format("Score: %.0f%% (%.0f%% played)",
                snapshot.overallAccuracyPercent(),
                snapshot.partialAccuracyPercent());
        scoreLabel.setText(scoreText);
        
        // Color code based on accuracy
        double accuracy = snapshot.partialAccuracyPercent();
        if (accuracy >= 90) {
            scoreLabel.setForeground(new Color(0, 150, 0)); // Green
        } else if (accuracy >= 70) {
            scoreLabel.setForeground(new Color(200, 150, 0)); // Yellow/Orange
        } else {
            scoreLabel.setForeground(new Color(200, 0, 0)); // Red
        }
    }
    
    // ==================== End Practice Mode Methods ====================
}
