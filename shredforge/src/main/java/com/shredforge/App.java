package com.shredforge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * ShredForge - Guitar Learning Application
 * Enhanced UI with real-time feedback and tab visualization
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("primary"), 1200, 800);
        
        // Load global stylesheet
        try {
            String css = App.class.getResource("/com/shredforge/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Error loading CSS file: " + e.getMessage());
            e.printStackTrace();
        }
        
        stage.setTitle("🎸 ShredForge - Guitar Learning Platform");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        
        stage.show();
        
        System.out.println("🎸 ShredForge started successfully!");
        System.out.println("📋 Ready to load tabs and start practicing!");
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void stop() {
        System.out.println("🎸 ShredForge shutting down...");
    }

    public static void main(String[] args) {
        launch();
    }
}
