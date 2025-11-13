package com.shredforge;

import javafx.fxml.FXML;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SecondaryController - Placeholder for future features
 * This can be used for settings, preferences, or additional views
 * 
 * @version 1.1
 * @author Team 2 - ShredForge
 */
public class SecondaryController {

    private static final Logger LOGGER = Logger.getLogger(SecondaryController.class.getName());

    @FXML
    public void initialize() {
        LOGGER.info("SecondaryController initialized");
    }

    @FXML
    private void switchToPrimary() {
        try {
            LOGGER.info("Navigating to primary view");
            App.setRoot("primary");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to switch to primary view", e);
        }
    }
}