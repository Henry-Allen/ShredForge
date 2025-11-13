package com.shredforge;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;

/**
 * Controller for the primary view of ShredForge.
 * This is the main interface for the music practice application.
 */
public class PrimaryController {

    private static final Logger LOGGER = Logger.getLogger(PrimaryController.class.getName());

    /**
     * Switches the view to the secondary screen.
     * Called when the user clicks the button in the primary view.
     */
    @FXML
    private void switchToSecondary() {
        try {
            App.setRoot("secondary");
            LOGGER.info("Switched to secondary view");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to switch to secondary view", e);
        }
    }
}
