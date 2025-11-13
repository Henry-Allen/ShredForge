package com.shredforge;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ShredForge - Music Practice and Training Application
 * Main application class that initializes and manages the JavaFX GUI.
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static final String APP_TITLE = "ShredForge";
    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Show splash screen while loading
        showSplashScreen(stage);
    }

    /**
     * Display splash screen with loading animation
     */
    private void showSplashScreen(Stage stage) {
        // Create splash screen
        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a0a, #1a1a2e); -fx-padding: 60;");

        // App title with glow effect
        Label titleLabel = new Label("ShredForge");
        titleLabel.setFont(new Font("System Bold", 48));
        titleLabel.setStyle("-fx-text-fill: #00d9ff; -fx-effect: dropshadow(gaussian, #00d9ff, 20, 0.7, 0, 0);");

        // Subtitle
        Label subtitleLabel = new Label("Guitar Practice & Training");
        subtitleLabel.setFont(new Font("System", 18));
        subtitleLabel.setStyle("-fx-text-fill: #00ff88;");

        // Loading label
        Label loadingLabel = new Label("Loading...");
        loadingLabel.setFont(new Font("System", 14));
        loadingLabel.setStyle("-fx-text-fill: white;");

        // Progress bar with gradient
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(8);

        splashLayout.getChildren().addAll(titleLabel, subtitleLabel, loadingLabel, progressBar);

        Scene splashScene = new Scene(splashLayout, 600, 400);

        // Apply theme to splash
        try {
            String stylesheet = App.class.getResource("modern-theme.css").toExternalForm();
            splashScene.getStylesheets().add(stylesheet);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load theme for splash screen", e);
        }

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(splashScene);
        stage.show();

        // Animate title
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(1000), titleLabel);
        scaleTransition.setFromX(0.5);
        scaleTransition.setFromY(0.5);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();

        // Simulate loading with animation
        new AnimationTimer() {
            private long startTime = -1;
            private final long DURATION = 2_000_000_000L; // 2 seconds in nanoseconds

            @Override
            public void handle(long now) {
                if (startTime < 0) {
                    startTime = now;
                }

                long elapsed = now - startTime;
                double progress = Math.min(1.0, (double) elapsed / DURATION);
                progressBar.setProgress(progress);

                if (progress >= 1.0) {
                    stop();
                    loadMainApplication(stage);
                }
            }
        }.start();
    }

    /**
     * Load the main application after splash screen
     */
    private void loadMainApplication(Stage stage) {
        try {
            // Create demo tabs if needed (first time user)
            com.shredforge.demo.DemoDataGenerator.createDemoTabsIfNeeded();

            scene = new Scene(loadFXML("mainmenu"), DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // Apply modern theme stylesheet
            try {
                String stylesheet = App.class.getResource("modern-theme.css").toExternalForm();
                scene.getStylesheets().add(stylesheet);
                LOGGER.info("Modern theme applied successfully");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load modern theme, using default", e);
            }

            // Hide stage temporarily to change style
            stage.hide();

            // Configure the primary stage
            stage.setTitle(APP_TITLE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setScene(scene);
            stage.setMinWidth(640);
            stage.setMinHeight(480);

            // Set up proper application closing
            stage.setOnCloseRequest(event -> {
                LOGGER.info("Application closing");
                cleanup();
                Platform.exit();
                System.exit(0);
            });

            // Fade transition from splash to main
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), scene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            stage.show();
            LOGGER.info("ShredForge application started successfully");

            // Show welcome dialog on first launch
            showWelcomeIfFirstLaunch();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showErrorAndExit("Failed to load application interface", e);
        }
    }

    /**
     * Show welcome dialog if this is the first launch
     */
    private void showWelcomeIfFirstLaunch() {
        Platform.runLater(() -> {
            com.shredforge.repository.ShredForgeRepository repository =
                com.shredforge.repository.ShredForgeRepository.getInstance();

            if (!repository.hasSeenWelcome()) {
                LOGGER.info("First launch detected - showing welcome dialog");
                com.shredforge.ui.DialogHelper.showWelcome();
                repository.markWelcomeSeen();
            }
        });
    }

    /**
     * Cleanup resources before exit
     */
    private void cleanup() {
        LOGGER.info("Cleaning up resources...");
        // Add any cleanup logic here (close audio streams, save state, etc.)
    }

    /**
     * Changes the root FXML view of the application
     * @param fxml The name of the FXML file (without .fxml extension)
     * @throws IOException if the FXML file cannot be loaded
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Loads an FXML file and returns its root node
     * @param fxml The name of the FXML file (without .fxml extension)
     * @return The root Parent node of the loaded FXML
     * @throws IOException if the FXML file cannot be loaded
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Shows an error dialog and exits the application
     * @param message The error message to display
     * @param e The exception that caused the error
     */
    private void showErrorAndExit(String message, Exception e) {
        LOGGER.log(Level.SEVERE, message, e);
        System.err.println(message + ": " + e.getMessage());
        Platform.exit();
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }

}