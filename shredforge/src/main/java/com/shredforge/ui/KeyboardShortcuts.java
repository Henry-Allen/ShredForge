package com.shredforge.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Centralized keyboard shortcut manager for the application.
 * Provides consistent keyboard shortcuts across all screens.
 */
public class KeyboardShortcuts {
    private static final Logger LOGGER = Logger.getLogger(KeyboardShortcuts.class.getName());

    // Define all keyboard shortcuts
    public static final KeyCombination PLAY_PAUSE = new KeyCodeCombination(KeyCode.SPACE);
    public static final KeyCombination STOP = new KeyCodeCombination(KeyCode.ESCAPE);
    public static final KeyCombination NEXT = new KeyCodeCombination(KeyCode.RIGHT);
    public static final KeyCombination PREVIOUS = new KeyCodeCombination(KeyCode.LEFT);
    public static final KeyCombination SPEED_UP = new KeyCodeCombination(KeyCode.UP);
    public static final KeyCombination SPEED_DOWN = new KeyCodeCombination(KeyCode.DOWN);
    public static final KeyCombination RESTART = new KeyCodeCombination(KeyCode.R);
    public static final KeyCombination LOOP = new KeyCodeCombination(KeyCode.L);
    public static final KeyCombination HELP = new KeyCodeCombination(KeyCode.F1);
    public static final KeyCombination SETTINGS = new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination CALIBRATE = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SEARCH = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

    private final Map<KeyCombination, Runnable> shortcuts;
    private Scene scene;

    public KeyboardShortcuts() {
        this.shortcuts = new HashMap<>();
        LOGGER.info("Keyboard shortcuts manager initialized");
    }

    /**
     * Register a keyboard shortcut
     * @param combination Key combination
     * @param action Action to execute
     */
    public void register(KeyCombination combination, Runnable action) {
        if (combination == null || action == null) {
            LOGGER.warning("Cannot register null shortcut or action");
            return;
        }

        shortcuts.put(combination, action);
        LOGGER.fine("Registered shortcut: " + combination);
    }

    /**
     * Attach shortcuts to a scene
     * @param scene Scene to attach shortcuts to
     */
    public void attach(Scene scene) {
        if (scene == null) {
            LOGGER.warning("Cannot attach shortcuts to null scene");
            return;
        }

        this.scene = scene;

        scene.setOnKeyPressed(event -> {
            for (Map.Entry<KeyCombination, Runnable> entry : shortcuts.entrySet()) {
                if (entry.getKey().match(event)) {
                    try {
                        event.consume();
                        entry.getValue().run();
                        LOGGER.fine("Executed shortcut: " + entry.getKey());
                    } catch (Exception e) {
                        LOGGER.warning("Error executing shortcut: " + e.getMessage());
                    }
                    break;
                }
            }
        });

        LOGGER.info("Keyboard shortcuts attached to scene");
    }

    /**
     * Detach shortcuts from current scene
     */
    public void detach() {
        if (scene != null) {
            scene.setOnKeyPressed(null);
            scene = null;
            LOGGER.info("Keyboard shortcuts detached");
        }
    }

    /**
     * Clear all registered shortcuts
     */
    public void clear() {
        shortcuts.clear();
        LOGGER.info("All shortcuts cleared");
    }

    /**
     * Get help text for all registered shortcuts
     */
    public String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Keyboard Shortcuts:\n\n");

        if (shortcuts.containsKey(PLAY_PAUSE)) {
            sb.append("Space - Play/Pause\n");
        }
        if (shortcuts.containsKey(STOP)) {
            sb.append("Escape - Stop\n");
        }
        if (shortcuts.containsKey(NEXT)) {
            sb.append("Right Arrow - Next Note\n");
        }
        if (shortcuts.containsKey(PREVIOUS)) {
            sb.append("Left Arrow - Previous Note\n");
        }
        if (shortcuts.containsKey(SPEED_UP)) {
            sb.append("Up Arrow - Speed Up\n");
        }
        if (shortcuts.containsKey(SPEED_DOWN)) {
            sb.append("Down Arrow - Speed Down\n");
        }
        if (shortcuts.containsKey(RESTART)) {
            sb.append("R - Restart\n");
        }
        if (shortcuts.containsKey(LOOP)) {
            sb.append("L - Toggle Loop\n");
        }
        if (shortcuts.containsKey(HELP)) {
            sb.append("F1 - Help\n");
        }
        if (shortcuts.containsKey(SETTINGS)) {
            sb.append("Ctrl+, - Settings\n");
        }
        if (shortcuts.containsKey(CALIBRATE)) {
            sb.append("Ctrl+C - Calibrate\n");
        }
        if (shortcuts.containsKey(SEARCH)) {
            sb.append("Ctrl+F - Search Tabs\n");
        }

        return sb.toString();
    }
}
