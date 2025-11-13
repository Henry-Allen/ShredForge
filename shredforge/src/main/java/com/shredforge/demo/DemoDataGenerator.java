package com.shredforge.demo;

import com.shredforge.model.Note;
import com.shredforge.model.Tab;
import com.shredforge.repository.ShredForgeRepository;
import com.shredforge.tab.TabManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates demo/sample tabs for first-time users.
 * Provides simple practice material to test the application.
 */
public class DemoDataGenerator {
    private static final Logger LOGGER = Logger.getLogger(DemoDataGenerator.class.getName());

    /**
     * Create and save demo tabs if no tabs exist
     */
    public static void createDemoTabsIfNeeded() {
        ShredForgeRepository repository = ShredForgeRepository.getInstance();
        TabManager tabManager = new TabManager();

        // Only create demo tabs if user has no tabs
        if (repository.getTabCount() == 0) {
            LOGGER.info("No tabs found - creating demo tabs");

            // Create simple beginner tabs
            Tab tab1 = createBeginnerTab1();
            Tab tab2 = createBeginnerTab2();
            Tab tab3 = createIntermediateTab();

            // Save tabs
            tabManager.setCurrentTab(tab1);
            repository.saveTab(tab1);

            tabManager.setCurrentTab(tab2);
            repository.saveTab(tab2);

            tabManager.setCurrentTab(tab3);
            repository.saveTab(tab3);

            LOGGER.info("Created 3 demo tabs");
        }
    }

    /**
     * Create a simple beginner tab - Open String Exercise
     */
    private static Tab createBeginnerTab1() {
        Tab tab = new Tab("demo-beginner-1", "Open Strings Exercise", "ShredForge Demo");
        tab.setDifficulty("Easy");
        tab.setTempo(60); // 60 BPM - slow
        tab.setRating(5.0f);
        tab.setDownloaded(true);

        List<Note> notes = new ArrayList<>();

        // Simple open string exercise: E-A-D-G-B-E pattern
        long time = 0;
        int[] strings = {6, 5, 4, 3, 2, 1}; // Low E to High E

        for (int repeat = 0; repeat < 4; repeat++) {
            for (int string : strings) {
                notes.add(new Note(getNoteNameForString(string), getOctaveForString(string),
                                 string, 0, time));
                time += 1000; // 1 second apart
            }
        }

        tab.setNotes(notes);
        tab.setDuration(time);

        LOGGER.info("Created demo tab: Open Strings Exercise");
        return tab;
    }

    /**
     * Create a simple beginner tab - Simple Scale
     */
    private static Tab createBeginnerTab2() {
        Tab tab = new Tab("demo-beginner-2", "Simple E Minor Scale", "ShredForge Demo");
        tab.setDifficulty("Easy");
        tab.setTempo(80);
        tab.setRating(4.5f);
        tab.setDownloaded(true);

        List<Note> notes = new ArrayList<>();

        // E minor pentatonic scale on low E string
        int[] frets = {0, 3, 5, 7, 10, 12, 10, 7, 5, 3, 0}; // Up and down
        long time = 0;

        for (int repeat = 0; repeat < 2; repeat++) {
            for (int fret : frets) {
                notes.add(new Note("E", 2, 6, fret, time));
                time += 750; // 750ms apart
            }
        }

        tab.setNotes(notes);
        tab.setDuration(time);

        LOGGER.info("Created demo tab: Simple E Minor Scale");
        return tab;
    }

    /**
     * Create an intermediate demo tab
     */
    private static Tab createIntermediateTab() {
        Tab tab = new Tab("demo-intermediate-1", "Beginner Riff", "ShredForge Demo");
        tab.setDifficulty("Medium");
        tab.setTempo(100);
        tab.setRating(4.0f);
        tab.setDownloaded(true);

        List<Note> notes = new ArrayList<>();

        // Simple repeating riff pattern
        int[][] pattern = {
            {6, 0}, {6, 3}, {6, 5}, {6, 3},  // E string
            {5, 0}, {5, 3}, {5, 5}, {5, 3},  // A string
            {6, 0}, {6, 3}, {6, 5}, {6, 7}   // Back to E string
        };

        long time = 0;
        for (int repeat = 0; repeat < 2; repeat++) {
            for (int[] note : pattern) {
                int string = note[0];
                int fret = note[1];
                notes.add(new Note(getNoteNameForString(string), getOctaveForString(string),
                                 string, fret, time));
                time += 500; // 500ms apart
            }
        }

        tab.setNotes(notes);
        tab.setDuration(time);

        LOGGER.info("Created demo tab: Beginner Riff");
        return tab;
    }

    /**
     * Get note name for open string
     */
    private static String getNoteNameForString(int string) {
        switch (string) {
            case 1: return "E"; // High E
            case 2: return "B";
            case 3: return "G";
            case 4: return "D";
            case 5: return "A";
            case 6: return "E"; // Low E
            default: return "E";
        }
    }

    /**
     * Get octave for open string
     */
    private static int getOctaveForString(int string) {
        switch (string) {
            case 1: return 4; // High E4
            case 2: return 3; // B3
            case 3: return 3; // G3
            case 4: return 3; // D3
            case 5: return 2; // A2
            case 6: return 2; // E2 (Low E)
            default: return 2;
        }
    }
}
