package com.shredforge;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        try {
            primaryStage = stage;

            // Create demo tabs if needed (first time user)
            com.shredforge.demo.DemoDataGenerator.createDemoTabsIfNeeded();

            scene = new Scene(loadFXML("mainmenu"), DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // Configure the primary stage
            stage.setTitle(APP_TITLE);
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

            stage.show();
            LOGGER.info("ShredForge application started successfully");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showErrorAndExit("Failed to load application interface", e);
        }
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
    static void setRoot(String fxml) throws IOException {
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