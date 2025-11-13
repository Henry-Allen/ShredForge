package com.shredforge;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;

/**
 * Controller for the secondary view of ShredForge.
 * This view provides additional functionality and settings.
 */
public class SecondaryController {

    private static final Logger LOGGER = Logger.getLogger(SecondaryController.class.getName());

    /**
     * Switches the view back to the primary screen.
     * Called when the user clicks the button in the secondary view.
     */
    @FXML
    private void switchToPrimary() {
        try {
            App.setRoot("primary");
            LOGGER.info("Switched to primary view");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to switch to primary view", e);
        }
    }
}