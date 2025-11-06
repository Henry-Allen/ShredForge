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
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static Scene scene;
    private static Stage primaryStage;
    private Stage splashStage;
    
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int MIN_WIDTH = 1000;
    private static final int MIN_HEIGHT = 700;
    private static final String APP_TITLE = "🎸 ShredForge - Guitar Learning Platform";
    
    private static final int SPLASH_WIDTH = 500;
    private static final int SPLASH_HEIGHT = 350;
    private static final int LOADING_STEPS = 10;
    private static final int STEP_DELAY_MS = 100;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            LOGGER.info("Starting ShredForge...");
            showSplashScreen();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showFatalError("Startup Error", e.toString());
        }
    }

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
        
        new Thread(this::performInitialization, "Init-Thread").start();
    }

    private VBox createSplashLayout() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a0a, #1a1a2e); " +
                       "-fx-padding: 60; -fx-background-radius: 15;");
        
        Label title = new Label("🎸 ShredForge");
        title.setFont(Font.font("System", FontWeight.BOLD, 48));
        title.setTextFill(Color.web("#e94560"));
        
        Label subtitle = new Label("Guitar Learning Platform");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.web("#00d9ff"));
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: linear-gradient(to right, #00ff88, #00d9ff);");
        
        Label status = new Label("Initializing...");
        status.setFont(Font.font("System", 14));
        status.setTextFill(Color.web("#888"));
        
        layout.getChildren().addAll(title, subtitle, progressBar, status);
        
        progressBar.setUserData(status);
        layout.setUserData(progressBar);
        
        return layout;
    }

    private void performInitialization() {
        try {
            VBox layout = (VBox) splashStage.getScene().getRoot();
            ProgressBar progressBar = (ProgressBar) layout.getUserData();
            Label status = (Label) progressBar.getUserData();
            
            for (int i = 0; i <= LOADING_STEPS; i++) {
                final int progress = i;
                final String statusText = "Loading... " + (progress * 10) + "%";
                
                Platform.runLater(() -> {
                    progressBar.setProgress(progress / (double) LOADING_STEPS);
                    status.setText(statusText);
                });
                
                Thread.sleep(STEP_DELAY_MS);
            }
            
            Platform.runLater(this::loadMainApplication);
            
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Initialization interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Initialization failed", e);
            Platform.runLater(() -> showFatalError("Init Error", e.toString()));
        }
    }

    private void loadMainApplication() {
        try {
            LOGGER.info("Loading main application...");
            
            Parent root = loadFXML("main");
            scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Try to load CSS
            try {
                URL cssUrl = App.class.getResource("styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    LOGGER.info("CSS loaded");
                }
            } catch (Exception e) {
                LOGGER.warning("Could not load CSS: " + e.getMessage());
            }
            
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setOnCloseRequest(event -> Platform.exit());
            
            transitionToMain();
            
            LOGGER.info("🎸 ShredForge started successfully!");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load main application", e);
            showFatalError("Load Error", "Could not load main interface:\n" + e.toString());
        }
    }

    private void transitionToMain() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splashStage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            splashStage.close();
            primaryStage.show();
            primaryStage.centerOnScreen();
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), scene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    public static void setRoot(String fxml) throws IOException {
        Parent newRoot = loadFXML(fxml);
        
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
    }

    private static Parent loadFXML(String fxml) throws IOException {
        LOGGER.info("Loading FXML: " + fxml);
        
        try {
            URL fxmlUrl = App.class.getResource(fxml + ".fxml");
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML file: " + fxml + ".fxml");
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            LOGGER.info("FXML loaded successfully: " + fxml);
            return root;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load FXML: " + fxml, e);
            throw new IOException("Failed to load " + fxml + ": " + e.getMessage(), e);
        }
    }

    private void showFatalError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Fatal Error");
            alert.setContentText(message + "\n\nThe application will now close.");
            if (primaryStage != null) {
                alert.initOwner(primaryStage);
            }
            alert.showAndWait();
            Platform.exit();
            System.exit(1);
        });
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down ShredForge...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}