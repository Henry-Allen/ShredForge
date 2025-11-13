package com.shredforge;

import com.shredforge.core.ShredforgeRepository;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final ShredforgeRepository REPOSITORY = ShredforgeRepository.builder().build();
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 720, 520);
        stage.setTitle("Shredforge Tester");
        stage.setScene(scene);
        stage.show();
    }

    public static ShredforgeRepository repository() {
        return REPOSITORY;
    }

    public static void showPrimary() {
        setRoot("primary");
    }

    public static void showCalibration() {
        setRoot("calibration");
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    private static void setRoot(String fxml) {
        try {
            Parent parent = loadFXML(fxml);
            scene.setRoot(parent);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load view: " + fxml, ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
