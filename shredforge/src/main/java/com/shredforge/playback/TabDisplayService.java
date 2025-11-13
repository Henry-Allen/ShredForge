package com.shredforge.playback;

import com.shredforge.model.Tab;
import com.shredforge.model.Note;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.logging.Logger;

/**
 * Displays guitar tablature on screen with scrolling and highlighting.
 * Per specification section 3.3.3
 */
public class TabDisplayService {
    private static final Logger LOGGER = Logger.getLogger(TabDisplayService.class.getName());

    private static final int STRING_SPACING = 30;
    private static final int NOTE_SPACING = 40;
    private static final int LEFT_MARGIN = 50;
    private static final int TOP_MARGIN = 100;

    private Tab displayedTab;
    private int scrollPosition;
    private Note highlightedNote;
    private Canvas canvas;

    public TabDisplayService(Canvas canvas) {
        this.canvas = canvas;
        this.scrollPosition = 0;
    }

    /**
     * Display a tab on the canvas
     */
    public void displayTab(Tab tab) {
        this.displayedTab = tab;
        this.scrollPosition = 0;
        this.highlightedNote = null;

        updateDisplay();
        LOGGER.info("Tab displayed: " + tab.getTitle());
    }

    /**
     * Update the display with current state
     */
    public void updateDisplay() {
        if (displayedTab == null || canvas == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw tab info
        drawTabInfo(gc);

        // Draw tab lines (6 strings)
        drawTabLines(gc);

        // Draw notes
        drawNotes(gc);

        // Draw current position marker
        drawPositionMarker(gc);
    }

    /**
     * Draw tab information header
     */
    private void drawTabInfo(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText(displayedTab.getTitle() + " - " + displayedTab.getArtist(),
                    LEFT_MARGIN, TOP_MARGIN - 60);

        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Difficulty: " + displayedTab.getDifficulty() +
                    " | Tempo: " + displayedTab.getTempo() + " BPM",
                    LEFT_MARGIN, TOP_MARGIN - 35);
    }

    /**
     * Draw the 6 tab lines representing guitar strings
     */
    private void drawTabLines(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        double width = canvas.getWidth() - LEFT_MARGIN * 2;

        for (int i = 0; i < 6; i++) {
            double y = TOP_MARGIN + (i * STRING_SPACING);
            gc.strokeLine(LEFT_MARGIN, y, LEFT_MARGIN + width, y);
        }

        // Draw string names
        gc.setFont(Font.font("Arial", 10));
        String[] stringNames = {"e", "B", "G", "D", "A", "E"};
        for (int i = 0; i < 6; i++) {
            double y = TOP_MARGIN + (i * STRING_SPACING) + 4;
            gc.fillText(stringNames[i], LEFT_MARGIN - 20, y);
        }
    }

    /**
     * Draw notes on the tab
     */
    private void drawNotes(GraphicsContext gc) {
        if (displayedTab == null || displayedTab.getNotes().isEmpty()) {
            return;
        }

        gc.setFont(Font.font("Arial", 12));

        for (int i = 0; i < displayedTab.getNotes().size(); i++) {
            Note note = displayedTab.getNotes().get(i);

            // Calculate position
            double x = LEFT_MARGIN + (i * NOTE_SPACING) - scrollPosition;

            // Only draw if visible
            if (x < LEFT_MARGIN - NOTE_SPACING || x > canvas.getWidth()) {
                continue;
            }

            int stringIndex = note.getString() - 1;  // 1-based to 0-based
            double y = TOP_MARGIN + (stringIndex * STRING_SPACING) + 4;

            // Highlight current note
            if (note.equals(highlightedNote)) {
                gc.setFill(Color.YELLOW);
                gc.fillOval(x - 8, y - 12, 20, 20);
            }

            // Draw fret number
            gc.setFill(Color.BLACK);
            gc.fillText(String.valueOf(note.getFret()), x, y);
        }
    }

    /**
     * Draw current position marker
     */
    private void drawPositionMarker(GraphicsContext gc) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(2);

        double x = canvas.getWidth() / 2;  // Center line
        double y1 = TOP_MARGIN - 20;
        double y2 = TOP_MARGIN + (5 * STRING_SPACING) + 20;

        gc.strokeLine(x, y1, x, y2);
    }

    /**
     * Scroll to specific position
     */
    public void scrollToPosition(int position) {
        this.scrollPosition = position;
        updateDisplay();
    }

    /**
     * Highlight a specific note
     */
    public void highlightNote(Note note) {
        this.highlightedNote = note;
        updateDisplay();
    }

    /**
     * Auto-scroll to keep current note centered
     */
    public void autoScroll() {
        if (highlightedNote == null || displayedTab == null) {
            return;
        }

        int noteIndex = displayedTab.getNotes().indexOf(highlightedNote);
        if (noteIndex >= 0) {
            int targetScroll = noteIndex * NOTE_SPACING - (int)(canvas.getWidth() / 2);
            scrollToPosition(Math.max(0, targetScroll));
        }
    }

    // Getters and setters
    public Tab getDisplayedTab() { return displayedTab; }
    public int getScrollPosition() { return scrollPosition; }
    public Note getHighlightedNote() { return highlightedNote; }
}
