package com.shredforge;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ShredForge - Guitar Learning Application
 * Main application entry point with enhanced splash screen and error handling
 * 
 * @version 1.1
 * @author Team 2 - William Allen, Daniel Marin, Alec Lovell, Kevin Perez
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static Scene scene;
    private static Stage primaryStage;
    private Stage splashStage;
    
    // Application constants
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int MIN_WIDTH = 1000;
    private static final int MIN_HEIGHT = 700;
    private static final String APP_TITLE = "🎸 ShredForge - Guitar Learning Platform";
    
    // Splash screen constants
    private static final int SPLASH_WIDTH = 500;
    private static final int SPLASH_HEIGHT = 350;
    private static final int LOADING_STEPS = 10;
    private static final int STEP_DELAY_MS = 150;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            setupExceptionHandler();
            showSplashScreen();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showFatalError("Application startup failed", e.getMessage());
        }
    }

    /**
     * Sets up global exception handler for uncaught exceptions
     */
    private void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread: " + thread.getName(), throwable);
            Platform.runLater(() -> showError("Unexpected Error", throwable.getMessage()));
        });
    }

    /**
     * Displays animated splash screen during application initialization
     */
    private void showSplashScreen() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        
        VBox splashLayout = createSplashLayout();
        
        Scene splashScene = new Scene(splashLayout, SPLASH_WIDTH, SPLASH_HEIGHT);
        splashScene.setFill(Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.setResizable(false);
        splashStage.centerOnScreen();
        splashStage.show();
        
        // Start loading process in background thread
        new Thread(this::performInitialization, "Initialization-Thread").start();
    }

    /**
     * Creates the splash screen layout with progress indicators
     */
    private VBox createSplashLayout() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a0a, #1a1a2e); -fx-padding: 60;");
        
        // Title
        Label title = new Label("🎸 ShredForge");
        title.setFont(Font.font("System", FontWeight.BOLD, 48));
        title.setTextFill(Color.web("#e94560"));
        
        // Subtitle
        Label subtitle = new Label("Guitar Learning Platform");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.web("#00d9ff"));
        
        // Progress bar
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: linear-gradient(to right, #00ff88, #00d9ff);");
        
        // Status label
        Label status = new Label("Initializing...");
        status.setFont(Font.font("System", 14));
        status.setTextFill(Color.web("#888"));
        
        layout.getChildren().addAll(title, subtitle, progressBar, status);
        
        // Store references for updates
        progressBar.setUserData(status);
        layout.setUserData(progressBar);
        
        return layout;
    }

    /**
     * Performs application initialization with progress updates
     */
    private void performInitialization() {
        try {
            VBox layout = (VBox) splashStage.getScene().getRoot();
            ProgressBar progressBar = (ProgressBar) layout.getUserData();
            Label status = (Label) progressBar.getUserData();
            
            for (int i = 0; i <= LOADING_STEPS; i++) {
                final int progress = i;
                final String statusText = getLoadingStatusText(progress);
                
                Platform.runLater(() -> {
                    progressBar.setProgress(progress / (double) LOADING_STEPS);
                    status.setText(statusText);
                });
                
                Thread.sleep(STEP_DELAY_MS);
                
                // Perform actual initialization at specific steps
                if (progress == 3) {
                    // Initialize audio system
                    LOGGER.info("Initializing audio system...");
                }
                if (progress == 6) {
                    // Load configuration
                    LOGGER.info("Loading configuration...");
                }
                if (progress == 9) {
                    // Prepare UI
                    LOGGER.info("Preparing user interface...");
                }
            }
            
            Platform.runLater(this::loadMainApplication);
            
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Initialization interrupted", e);
            Thread.currentThread().interrupt();
            Platform.runLater(() -> showError("Initialization Error", "Application loading was interrupted"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Initialization failed", e);
            Platform.runLater(() -> showFatalError("Initialization Failed", e.getMessage()));
        }
    }

    /**
     * Returns appropriate status text for loading progress
     */
    private String getLoadingStatusText(int progress) {
        if (progress == 0) return "Initializing...";
        if (progress <= 2) return "Loading resources...";
        if (progress <= 5) return "Initializing audio engine...";
        if (progress <= 8) return "Loading configuration...";
        if (progress <= 9) return "Preparing UI...";
        return "Almost ready...";
    }

    /**
     * Loads the main application window after initialization
     */
    private void loadMainApplication() {
        try {
            // Load primary view (dashboard)
            scene = new Scene(loadFXML("main"), WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Load global stylesheet
            String css = App.class.getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            
            // Configure primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            
            // Setup close handler
            primaryStage.setOnCloseRequest(event -> handleApplicationClose());
            
            // Perform transition from splash to main
            transitionToMain();
            
            LOGGER.info("🎸 ShredForge started successfully!");
            LOGGER.info("📋 Ready to load tabs and start practicing!");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load main application", e);
            showFatalError("Load Error", "Could not load the main application interface");
        }
    }

    /**
     * Handles smooth transition from splash screen to main window
     */
    private void transitionToMain() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashStage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            splashStage.close();
            primaryStage.show();
            primaryStage.centerOnScreen();
            
            // Fade in main stage
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), scene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /**
     * Changes the root scene to a different FXML view with transition
     * 
     * @param fxml The name of the FXML file (without .fxml extension)
     * @throws IOException if the FXML file cannot be loaded
     */
    public static void setRoot(String fxml) throws IOException {
        Parent newRoot = loadFXML(fxml);
        
        // Fade transition between views
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), scene.getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            scene.setRoot(newRoot);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
        
        LOGGER.info("View changed to: " + fxml);
    }

    /**
     * Loads an FXML file and returns the root node
     * 
     * @param fxml The name of the FXML file (without .fxml extension)
     * @return The loaded Parent node
     * @throws IOException if the file cannot be loaded
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load FXML: " + fxml, e);
            throw e;
        }
    }

    /**
     * Handles application close event
     */
    private void handleApplicationClose() {
        LOGGER.info("🎸 ShredForge shutting down...");
        // Perform cleanup here (close audio streams, save settings, etc.)
        // This will be called automatically on application exit
    }

    @Override
    public void stop() {
        LOGGER.info("👋 Thanks for practicing! Keep shredding!");
        // Additional cleanup if needed
    }

    /**
     * Shows an error dialog to the user
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a fatal error and exits the application
     */
    private void showFatalError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Fatal Error");
        alert.setContentText(message + "\n\nThe application will now close.");
        alert.showAndWait();
        Platform.exit();
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        LOGGER.info("Starting ShredForge application...");
        launch(args);
    }
}