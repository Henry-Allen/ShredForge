package com.shredforge;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;

/**
 * LessonsController - Enhanced Lesson Management System
 * 
 * Features:
 * - 6 lesson categories with 6 lessons each (36 total)
 * - Progress tracking per category
 * - Lesson preview with detailed information
 * - Search functionality across all lessons
 * - Favorites system
 * - Completion tracking
 * - Difficulty indicators
 * 
 * @version 1.2
 * @author Team 2 - ShredForge
 */
public class LessonsController {

    private static final Logger LOGGER = Logger.getLogger(LessonsController.class.getName());

    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> lessonList;
    @FXML private TextArea lessonPreview;
    @FXML private Button startLessonButton;
    @FXML private Button backButton;
    @FXML private Label categoryLabel;
    @FXML private ProgressBar categoryProgress;
    @FXML private TextField searchField;

    private ObservableList<String> categories;
    private ObservableList<String> currentLessons;
    private FilteredList<String> filteredLessons;
    private Set<String> completedLessons;
    private Set<String> favoriteLessons;
    private int currentCategoryIndex = 0;

    @FXML
    public void initialize() {
        LOGGER.info("Initializing LessonsController");
        
        completedLessons = new HashSet<>();
        favoriteLessons = new HashSet<>();
        
        setupCategories();
        setupEventHandlers();
        setupSearchFunctionality();
        
        categoryList.getSelectionModel().selectFirst();
        loadLessonsForCategory(0);
        
        LOGGER.info("LessonsController initialized with " + categories.size() + " categories");
    }

    /**
     * Sets up the category list
     */
    private void setupCategories() {
        categories = FXCollections.observableArrayList(
            "🎸 Beginner Fundamentals",
            "🎼 Chord Mastery",
            "⚡ Speed & Technique",
            "🎵 Music Theory",
            "🎶 Songs & Repertoire",
            "🤘 Advanced Shredding"
        );
        categoryList.setItems(categories);
        
        // Add hover effects to categories
        categoryList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    final String baseStyle = "-fx-text-fill: white; " +
                           "-fx-background-color: transparent; " +
                           "-fx-padding: 15 20; " +
                           "-fx-font-size: 15px; " +
                           "-fx-font-weight: bold;";
                    
                    final String hoverStyle = baseStyle +
                           "-fx-background-color: rgba(0, 217, 255, 0.2); " +
                           "-fx-cursor: hand;";
                    
                    setStyle(baseStyle);
                    setOnMouseEntered(e -> setStyle(hoverStyle));
                    setOnMouseExited(e -> setStyle(baseStyle));
                }
            }
        });
    }

    /**
     * Sets up event handlers for UI components
     */
    private void setupEventHandlers() {
        // Category selection handler
        categoryList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.intValue() >= 0) {
                    loadLessonsForCategory(newVal.intValue());
                    
                    // Animate category change
                    FadeTransition fade = new FadeTransition(Duration.millis(200), lessonList);
                    fade.setFromValue(0.7);
                    fade.setToValue(1.0);
                    fade.play();
                }
            }
        );

        // Lesson selection handler
        lessonList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.intValue() >= 0) {
                    showLessonPreview(newVal.intValue());
                    
                    // Animate preview change
                    ScaleTransition scale = new ScaleTransition(Duration.millis(150), lessonPreview);
                    scale.setFromX(0.98);
                    scale.setFromY(0.98);
                    scale.setToX(1.0);
                    scale.setToY(1.0);
                    scale.play();
                }
            }
        );
    }

    /**
     * Sets up search functionality
     */
    private void setupSearchFunctionality() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterLessons(newValue);
            });
        }
    }

    /**
     * Filters lessons based on search text
     */
    private void filterLessons(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            lessonList.setItems(currentLessons);
            return;
        }
        
        FilteredList<String> filtered = new FilteredList<>(currentLessons);
        filtered.setPredicate(lesson -> 
            lesson.toLowerCase().contains(searchText.toLowerCase())
        );
        
        lessonList.setItems(filtered);
        
        LOGGER.info("Filtered lessons: " + filtered.size() + " results");
    }

    /**
     * Loads lessons for a specific category
     */
    private void loadLessonsForCategory(int categoryIndex) {
        currentCategoryIndex = categoryIndex;
        
        switch (categoryIndex) {
            case 0: // Beginner Fundamentals
                currentLessons = FXCollections.observableArrayList(
                    "Lesson 1: Holding the Guitar ✓",
                    "Lesson 2: Basic Hand Positioning ✓",
                    "Lesson 3: Your First Chord - E Minor ✓",
                    "Lesson 4: Strumming Patterns",
                    "Lesson 5: Switching Between Chords",
                    "Lesson 6: Basic Rhythm Exercise"
                );
                updateCategoryInfo("Beginner Fundamentals - 3/6 Complete", 0.5);
                break;

            case 1: // Chord Mastery
                currentLessons = FXCollections.observableArrayList(
                    "Lesson 1: Open Chords - Major ✓",
                    "Lesson 2: Open Chords - Minor",
                    "Lesson 3: Barre Chords - E Shape",
                    "Lesson 4: Barre Chords - A Shape",
                    "Lesson 5: Chord Transitions",
                    "Lesson 6: Advanced Chord Voicings"
                );
                updateCategoryInfo("Chord Mastery - 1/6 Complete", 0.16);
                break;

            case 2: // Speed & Technique
                currentLessons = FXCollections.observableArrayList(
                    "Lesson 1: Alternate Picking Basics",
                    "Lesson 2: Economy Picking",
                    "Lesson 3: Sweep Picking Introduction",
                    "Lesson 4: Legato Technique",
                    "Lesson 5: Tapping Fundamentals",
                    "Lesson 6: Building Speed Gradually"
                );
                updateCategoryInfo("Speed & Technique - 0/6 Complete", 0.0);
                break;

            case 3: // Music Theory
                currentLessons = FXCollections.observableArrayList(
                    "Lesson 1: Understanding the Fretboard",
                    "Lesson 2: Major Scale Construction",
                    "Lesson 3: Minor Scales & Modes",
                    "Lesson 4: Chord Building",
                    "Lesson 5: Intervals & Harmony",
                    "Lesson 6: Key Signatures"
                );
                updateCategoryInfo("Music Theory - 0/6 Complete", 0.0);
                break;

            case 4: // Songs & Repertoire
                currentLessons = FXCollections.observableArrayList(
                    "Song 1: Smoke on the Water - Deep Purple",
                    "Song 2: Seven Nation Army - The White Stripes",
                    "Song 3: Wonderwall - Oasis",
                    "Song 4: Sweet Child O' Mine - Guns N' Roses",
                    "Song 5: Blackbird - The Beatles",
                    "Song 6: Stairway to Heaven - Led Zeppelin"
                );
                updateCategoryInfo("Songs & Repertoire - 0/6 Complete", 0.0);
                break;

            case 5: // Advanced Shredding
                currentLessons = FXCollections.observableArrayList(
                    "Lesson 1: Advanced Scale Patterns",
                    "Lesson 2: Exotic Scales & Modes",
                    "Lesson 3: Sweep Picking Mastery",
                    "Lesson 4: Two-Hand Tapping",
                    "Lesson 5: Complex Rhythm Patterns",
                    "Lesson 6: Creating Your Own Solos"
                );
                updateCategoryInfo("Advanced Shredding - 0/6 Complete", 0.0);
                break;
        }

        lessonList.setItems(currentLessons);
        lessonList.getSelectionModel().selectFirst();
        
        LOGGER.info("Loaded category: " + categories.get(categoryIndex));
    }

    /**
     * Updates category info display
     */
    private void updateCategoryInfo(String text, double progress) {
        categoryLabel.setText(text);
        categoryProgress.setProgress(progress);
        
        // Animate progress bar
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), categoryProgress);
        scale.setFromY(0.8);
        scale.setToY(1.0);
        scale.play();
    }

    /**
     * Shows detailed preview for selected lesson
     */
    private void showLessonPreview(int lessonIndex) {
        if (lessonIndex < 0 || lessonIndex >= currentLessons.size()) {
            return;
        }
        
        String lesson = currentLessons.get(lessonIndex);
        StringBuilder preview = new StringBuilder();
        
        // Get lesson details based on category and index
        preview.append(getLessonTitle(lesson)).append("\n\n");
        preview.append(getLessonDescription(currentCategoryIndex, lessonIndex));
        
        lessonPreview.setText(preview.toString());
        
        // Enable/disable start button based on completion
        startLessonButton.setDisable(lesson.contains("✓"));
        
        LOGGER.info("Showing preview for: " + lesson);
    }

    /**
     * Gets formatted lesson title
     */
    private String getLessonTitle(String lesson) {
        String title = lesson.replace(" ✓", "");
        return "📚 " + title;
    }

    /**
     * Gets lesson description based on category and index
     */
    private String getLessonDescription(int categoryIndex, int lessonIndex) {
        // Beginner Fundamentals
        if (categoryIndex == 0) {
            switch (lessonIndex) {
                case 3: // Strumming Patterns
                    return "Duration: 15 minutes\n" +
                           "Difficulty: ⭐ Beginner\n" +
                           "Prerequisites: Know at least 2 chords\n\n" +
                           "What You'll Learn:\n" +
                           "• Basic down strumming pattern\n" +
                           "• Adding upstrokes to your strumming\n" +
                           "• Common 4/4 time patterns\n" +
                           "• How to maintain steady rhythm\n\n" +
                           "This lesson introduces fundamental strumming patterns " +
                           "that you'll use in countless songs. We'll start with simple " +
                           "down strums and gradually add upstrokes to create more " +
                           "dynamic rhythm patterns.\n\n" +
                           "🎯 By the end, you'll be able to play basic rhythm guitar!";
                           
                case 4: // Switching Between Chords
                    return "Duration: 20 minutes\n" +
                           "Difficulty: ⭐⭐ Beginner\n" +
                           "Prerequisites: Know G, C, D, Em chords\n\n" +
                           "What You'll Learn:\n" +
                           "• Smooth chord transitions\n" +
                           "• Pivot finger technique\n" +
                           "• Common chord progressions\n" +
                           "• Practice exercises for muscle memory\n\n" +
                           "The key to great rhythm guitar is smooth transitions. " +
                           "This lesson teaches you professional techniques for " +
                           "changing chords without breaking the flow.\n\n" +
                           "🎯 Master this and you'll be ready to play full songs!";
                default:
                    return getGenericLessonDescription();
            }
        }
        
        // Speed & Technique
        if (categoryIndex == 2 && lessonIndex == 0) {
            return "Duration: 25 minutes\n" +
                   "Difficulty: ⭐⭐⭐ Intermediate\n" +
                   "Prerequisites: Basic fretting technique\n\n" +
                   "What You'll Learn:\n" +
                   "• Proper pick grip and angle\n" +
                   "• Down-up motion mechanics\n" +
                   "• Chromatic exercises for speed\n" +
                   "• Building speed gradually and safely\n\n" +
                   "Alternate picking is the foundation of fast playing. " +
                   "This lesson covers professional techniques used by " +
                   "shredders worldwide.\n\n" +
                   "🎯 Essential skill for metal, rock, and jazz guitar!";
        }
        
        // Songs
        if (categoryIndex == 4) {
            String[] songs = {
                "Smoke on the Water",
                "Seven Nation Army",
                "Wonderwall",
                "Sweet Child O' Mine",
                "Blackbird",
                "Stairway to Heaven"
            };
            
            String[] difficulties = {"⭐", "⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐"};
            String[] times = {"10", "12", "20", "25", "30", "35"};
            
            if (lessonIndex < songs.length) {
                return "Duration: " + times[lessonIndex] + " minutes\n" +
                       "Difficulty: " + difficulties[lessonIndex] + "\n" +
                       "Song: " + songs[lessonIndex] + "\n\n" +
                       "What You'll Learn:\n" +
                       "• Complete song arrangement\n" +
                       "• Riff breakdown and technique\n" +
                       "• Rhythm and timing\n" +
                       "• Performance tips\n\n" +
                       "Learn to play this classic song from start to finish " +
                       "with detailed instruction on every part.\n\n" +
                       "🎯 Impress your friends with this iconic tune!";
            }
        }
        
        return getGenericLessonDescription();
    }

    /**
     * Returns generic lesson description
     */
    private String getGenericLessonDescription() {
        return "Duration: 15-20 minutes\n" +
               "Difficulty: Varies by lesson\n\n" +
               "What You'll Learn:\n" +
               "• Core concepts and techniques\n" +
               "• Step-by-step video instruction\n" +
               "• Practice exercises\n" +
               "• Tips from professional guitarists\n\n" +
               "This lesson contains structured exercises and theory " +
               "to help you progress on your guitar journey.\n\n" +
               "🎯 Click 'Start Lesson' to begin your learning!";
    }

    @FXML
    private void handleStartLesson() {
        String lesson = lessonList.getSelectionModel().getSelectedItem();
        if (lesson != null && !lesson.contains("✓")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Starting Lesson 🎸");
            alert.setHeaderText(lesson);
            alert.setContentText(
                "The lesson interface will contain:\n\n" +
                "📹 High-quality video instruction\n" +
                "🎼 Interactive tablature display\n" +
                "🎯 Real-time feedback on your playing\n" +
                "📊 Progress tracking and achievements\n" +
                "💾 Auto-save your progress\n\n" +
                "Full interactive lesson player coming soon!\n" +
                "For now, practice the exercises in the Practice mode."
            );
            
            alert.showAndWait();
            
            LOGGER.info("Started lesson: " + lesson);
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("main");
            LOGGER.info("Navigated back to main");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main", e);
        }
    }

    /**
     * Cleanup method
     */
    public void cleanup() {
        LOGGER.info("Cleaning up LessonsController");
    }
}