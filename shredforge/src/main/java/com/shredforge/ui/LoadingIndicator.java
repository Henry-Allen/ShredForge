package com.shredforge.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.logging.Logger;

/**
 * Reusable loading indicator for long-running operations.
 * Shows a modal dialog with progress indicator and message.
 */
public class LoadingIndicator {
    private static final Logger LOGGER = Logger.getLogger(LoadingIndicator.class.getName());

    private final Stage dialog;
    private final Label messageLabel;
    private final ProgressBar progressBar;
    private final ProgressIndicator progressIndicator;
    private boolean showProgress;

    /**
     * Create loading indicator with message only
     */
    public LoadingIndicator(String title, String message) {
        this(title, message, false);
    }

    /**
     * Create loading indicator with optional progress bar
     * @param title Dialog title
     * @param message Message to display
     * @param showProgress Whether to show determinate progress bar
     */
    public LoadingIndicator(String title, String message, boolean showProgress) {
        this.showProgress = showProgress;

        dialog = new Stage();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setResizable(false);

        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-padding: 30; -fx-background-color: white;");

        messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px;");

        if (showProgress) {
            progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(300);
            progressIndicator = null;
            vbox.getChildren().addAll(messageLabel, progressBar);
        } else {
            progressBar = null;
            progressIndicator = new ProgressIndicator();
            progressIndicator.setProgress(-1); // Indeterminate
            vbox.getChildren().addAll(messageLabel, progressIndicator);
        }

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);

        LOGGER.fine("Loading indicator created: " + title);
    }

    /**
     * Show the loading indicator
     */
    public void show() {
        Platform.runLater(() -> {
            try {
                dialog.show();
                LOGGER.fine("Loading indicator shown");
            } catch (Exception e) {
                LOGGER.warning("Error showing loading indicator: " + e.getMessage());
            }
        });
    }

    /**
     * Hide the loading indicator
     */
    public void hide() {
        Platform.runLater(() -> {
            try {
                dialog.hide();
                LOGGER.fine("Loading indicator hidden");
            } catch (Exception e) {
                LOGGER.warning("Error hiding loading indicator: " + e.getMessage());
            }
        });
    }

    /**
     * Update the message
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    /**
     * Update progress (only works if showProgress was true)
     * @param progress Progress value between 0.0 and 1.0
     */
    public void updateProgress(double progress) {
        if (showProgress && progressBar != null) {
            Platform.runLater(() -> progressBar.setProgress(Math.max(0.0, Math.min(1.0, progress))));
        }
    }

    /**
     * Update both message and progress
     */
    public void update(String message, double progress) {
        updateMessage(message);
        updateProgress(progress);
    }

    /**
     * Check if dialog is showing
     */
    public boolean isShowing() {
        return dialog.isShowing();
    }
}
