package com.shredforge.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Helper class for showing user-friendly dialogs.
 * Provides confirmation, error, info, and warning dialogs.
 */
public class DialogHelper {
    private static final Logger LOGGER = Logger.getLogger(DialogHelper.class.getName());

    /**
     * Show confirmation dialog
     * @param title Dialog title
     * @param message Dialog message
     * @return true if user clicked OK/Yes
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmed = result.isPresent() && result.get() == ButtonType.OK;

        LOGGER.fine("Confirmation dialog: " + title + " - " + (confirmed ? "Confirmed" : "Cancelled"));
        return confirmed;
    }

    /**
     * Show error dialog with user-friendly message
     * @param title Dialog title
     * @param message User-friendly error message
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Oops! Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();

        LOGGER.warning("Error dialog shown: " + title + " - " + message);
    }

    /**
     * Show error with suggested action
     * @param title Dialog title
     * @param message Error message
     * @param suggestion Suggested action to fix the problem
     */
    public static void showErrorWithSuggestion(String title, String message, String suggestion) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText("Suggestion: " + suggestion);
        alert.showAndWait();

        LOGGER.warning("Error dialog shown: " + title + " - " + message);
    }

    /**
     * Show information dialog
     * @param title Dialog title
     * @param message Information message
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        LOGGER.info("Info dialog shown: " + title + " - " + message);
    }

    /**
     * Show warning dialog
     * @param title Dialog title
     * @param message Warning message
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        LOGGER.warning("Warning dialog shown: " + title + " - " + message);
    }

    /**
     * Show success message
     * @param title Dialog title
     * @param message Success message
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Success!");
        alert.setContentText(message);
        alert.showAndWait();

        LOGGER.info("Success dialog shown: " + title + " - " + message);
    }

    /**
     * Show calibration instructions
     */
    public static void showCalibrationHelp() {
        showInfo("Calibration Help",
            "Calibration helps ShredForge detect your guitar accurately.\n\n" +
            "Steps:\n" +
            "1. Tune your guitar to standard tuning\n" +
            "2. Play each open string when prompted\n" +
            "3. Play it cleanly and let it ring for 2 seconds\n" +
            "4. Wait for confirmation before moving to next string\n\n" +
            "Tip: Use a guitar tuner first for best results!");
    }

    /**
     * Show first-time welcome message
     */
    public static void showWelcome() {
        showInfo("Welcome to ShredForge!",
            "ShredForge helps you practice guitar with real-time feedback.\n\n" +
            "Quick Start:\n" +
            "1. Calibrate your guitar (one-time setup)\n" +
            "2. Browse or search for tabs\n" +
            "3. Start practicing and improve your skills!\n\n" +
            "We've added some demo tabs to get you started.");
    }

    /**
     * Show keyboard shortcuts
     */
    public static void showKeyboardShortcuts() {
        showInfo("Keyboard Shortcuts",
            "Practice Session:\n" +
            "• Space - Play/Pause\n" +
            "• Escape - Stop\n" +
            "• R - Restart\n" +
            "• Arrow Up/Down - Speed up/down\n\n" +
            "General:\n" +
            "• Ctrl+F - Search tabs\n" +
            "• Ctrl+, - Settings\n" +
            "• Ctrl+C - Calibrate\n" +
            "• F1 - Help");
    }

    /**
     * Show about dialog with app information
     */
    public static void showAbout() {
        showInfo("About ShredForge",
            "ShredForge v1.0\n\n" +
            "Guitar Practice & Training Application\n\n" +
            "Features:\n" +
            "• Real-time note detection\n" +
            "• Guitar tablature practice\n" +
            "• Progress tracking\n" +
            "• Audio input calibration\n\n" +
            "Developed for music education and practice.\n\n" +
            "© 2024 ShredForge Team");
    }
}
