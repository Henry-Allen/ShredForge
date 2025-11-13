package com.shredforge.ui;

import com.shredforge.App;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the lessons view
 * Provides structured guitar lessons organized by category
 */
public class LessonsController {

    private static final Logger LOGGER = Logger.getLogger(LessonsController.class.getName());

    @FXML
    private ChoiceBox<String> categoryChoiceBox;

    @FXML
    private VBox lessonsListBox;

    @FXML
    private TextField searchField;

    @FXML
    private Label categoryDescriptionLabel;

    private Map<String, List<Lesson>> lessonsByCategory;
    private Set<String> completedLessons = new HashSet<>();

    @FXML
    public void initialize() {
        initializeLessons();
        setupCategorySelector();
        setupSearch();

        // Apply fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300));
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        if (lessonsListBox != null) {
            fadeIn.setNode(lessonsListBox);
            fadeIn.play();
        }
    }

    /**
     * Initialize lesson categories and content
     */
    private void initializeLessons() {
        lessonsByCategory = new LinkedHashMap<>();

        // Beginner Lessons
        List<Lesson> beginnerLessons = new ArrayList<>();
        beginnerLessons.add(new Lesson("Holding the Guitar", "Learn proper guitar posture and hand positioning", "Beginner", 10));
        beginnerLessons.add(new Lesson("Open String Notes", "Memorize the names of all six strings", "Beginner", 5));
        beginnerLessons.add(new Lesson("Fretting Basics", "How to press strings cleanly on the fretboard", "Beginner", 15));
        beginnerLessons.add(new Lesson("Basic Chord Shapes", "Learn E minor, A minor, and D major chords", "Beginner", 20));
        beginnerLessons.add(new Lesson("Strumming Patterns", "Master basic downstroke and upstroke patterns", "Beginner", 15));
        beginnerLessons.add(new Lesson("Chord Transitions", "Practice smooth changes between chords", "Beginner", 20));
        lessonsByCategory.put("Beginner", beginnerLessons);

        // Technique Lessons
        List<Lesson> techniqueLessons = new ArrayList<>();
        techniqueLessons.add(new Lesson("Alternate Picking", "Develop efficient picking hand technique", "Intermediate", 25));
        techniqueLessons.add(new Lesson("Hammer-Ons & Pull-Offs", "Learn legato techniques for smooth playing", "Intermediate", 20));
        techniqueLessons.add(new Lesson("String Bending", "Master pitch bending for expressive playing", "Intermediate", 30));
        techniqueLessons.add(new Lesson("Vibrato Technique", "Add emotional depth with finger vibrato", "Intermediate", 15));
        techniqueLessons.add(new Lesson("Palm Muting", "Control dynamics with palm muting", "Intermediate", 20));
        techniqueLessons.add(new Lesson("Slides & Glissando", "Connect notes smoothly with slides", "Intermediate", 15));
        lessonsByCategory.put("Technique", techniqueLessons);

        // Scales Lessons
        List<Lesson> scalesLessons = new ArrayList<>();
        scalesLessons.add(new Lesson("Pentatonic Scale Shapes", "Learn all 5 pentatonic positions", "Intermediate", 30));
        scalesLessons.add(new Lesson("Major Scale Patterns", "Master the major scale across the neck", "Intermediate", 35));
        scalesLessons.add(new Lesson("Natural Minor Scale", "Explore minor tonality and patterns", "Intermediate", 30));
        scalesLessons.add(new Lesson("Blues Scale", "Add blues flavor with the blues scale", "Intermediate", 25));
        scalesLessons.add(new Lesson("Harmonic Minor", "Introduce exotic sounds with harmonic minor", "Advanced", 30));
        scalesLessons.add(new Lesson("Modes Introduction", "Understand modal scales and their sounds", "Advanced", 40));
        lessonsByCategory.put("Scales", scalesLessons);

        // Rhythm Lessons
        List<Lesson> rhythmLessons = new ArrayList<>();
        rhythmLessons.add(new Lesson("Understanding Time Signatures", "Master 4/4, 3/4, and 6/8 time", "Beginner", 15));
        rhythmLessons.add(new Lesson("Reading Rhythm Notation", "Interpret quarter, eighth, and sixteenth notes", "Beginner", 20));
        rhythmLessons.add(new Lesson("Syncopation Basics", "Play off-beat rhythms with confidence", "Intermediate", 25));
        rhythmLessons.add(new Lesson("Triplets & Swing Feel", "Master triplet rhythms and swing timing", "Intermediate", 25));
        rhythmLessons.add(new Lesson("Advanced Strumming", "Complex strumming patterns and accents", "Intermediate", 30));
        rhythmLessons.add(new Lesson("Fingerstyle Patterns", "Develop fingerpicking rhythms", "Advanced", 35));
        lessonsByCategory.put("Rhythm", rhythmLessons);

        // Lead Guitar Lessons
        List<Lesson> leadLessons = new ArrayList<>();
        leadLessons.add(new Lesson("Building Melodic Lines", "Create memorable guitar melodies", "Intermediate", 30));
        leadLessons.add(new Lesson("Phrasing Concepts", "Structure solos with call and response", "Intermediate", 25));
        leadLessons.add(new Lesson("Speed Building Exercises", "Develop faster, cleaner playing", "Advanced", 40));
        leadLessons.add(new Lesson("Sweep Picking Basics", "Introduction to sweep picking arpeggios", "Advanced", 35));
        leadLessons.add(new Lesson("Tapping Technique", "Master two-hand tapping patterns", "Advanced", 30));
        leadLessons.add(new Lesson("Solo Construction", "Build complete, dynamic guitar solos", "Advanced", 45));
        lessonsByCategory.put("Lead Guitar", leadLessons);

        // Music Theory Lessons
        List<Lesson> theoryLessons = new ArrayList<>();
        theoryLessons.add(new Lesson("Notes on the Fretboard", "Memorize all notes across all strings", "Beginner", 25));
        theoryLessons.add(new Lesson("Intervals Explained", "Understand musical distance between notes", "Intermediate", 20));
        theoryLessons.add(new Lesson("Chord Construction", "Build triads and seventh chords", "Intermediate", 30));
        theoryLessons.add(new Lesson("Circle of Fifths", "Navigate keys and chord progressions", "Intermediate", 25));
        theoryLessons.add(new Lesson("Diatonic Harmony", "Understand chords within a key", "Advanced", 35));
        theoryLessons.add(new Lesson("Voice Leading", "Create smooth chord progressions", "Advanced", 30));
        lessonsByCategory.put("Music Theory", theoryLessons);
    }

    /**
     * Setup the category selector dropdown
     */
    private void setupCategorySelector() {
        if (categoryChoiceBox != null) {
            categoryChoiceBox.getItems().addAll(lessonsByCategory.keySet());
            categoryChoiceBox.setValue("Beginner");

            categoryChoiceBox.setOnAction(event -> {
                String selectedCategory = categoryChoiceBox.getValue();
                displayLessonsForCategory(selectedCategory);
                updateCategoryDescription(selectedCategory);
            });

            // Display initial category
            displayLessonsForCategory("Beginner");
            updateCategoryDescription("Beginner");
        }
    }

    /**
     * Setup search functionality
     */
    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterLessons(newValue);
            });
        }
    }

    /**
     * Update category description
     */
    private void updateCategoryDescription(String category) {
        if (categoryDescriptionLabel == null) return;

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("Beginner", "Start your guitar journey with fundamental techniques and basic chords");
        descriptions.put("Technique", "Develop advanced playing techniques for greater expression and speed");
        descriptions.put("Scales", "Master essential scales and patterns across the entire fretboard");
        descriptions.put("Rhythm", "Improve your timing and rhythmic vocabulary");
        descriptions.put("Lead Guitar", "Learn to craft engaging solos and melodic lines");
        descriptions.put("Music Theory", "Understand the theory behind the music you play");

        categoryDescriptionLabel.setText(descriptions.getOrDefault(category, "Explore guitar lessons"));
    }

    /**
     * Display lessons for the selected category
     */
    private void displayLessonsForCategory(String category) {
        if (lessonsListBox == null) return;

        lessonsListBox.getChildren().clear();

        List<Lesson> lessons = lessonsByCategory.get(category);
        if (lessons == null || lessons.isEmpty()) {
            Label emptyLabel = new Label("No lessons available");
            emptyLabel.setStyle("-fx-text-fill: #888;");
            lessonsListBox.getChildren().add(emptyLabel);
            return;
        }

        for (Lesson lesson : lessons) {
            lessonsListBox.getChildren().add(createLessonCard(lesson));
        }
    }

    /**
     * Filter lessons based on search query
     */
    private void filterLessons(String query) {
        if (lessonsListBox == null || query == null) return;

        lessonsListBox.getChildren().clear();

        if (query.trim().isEmpty()) {
            String currentCategory = categoryChoiceBox != null ? categoryChoiceBox.getValue() : "Beginner";
            displayLessonsForCategory(currentCategory);
            return;
        }

        String lowerQuery = query.toLowerCase();
        boolean foundAny = false;

        for (Map.Entry<String, List<Lesson>> entry : lessonsByCategory.entrySet()) {
            for (Lesson lesson : entry.getValue()) {
                if (lesson.getTitle().toLowerCase().contains(lowerQuery) ||
                    lesson.getDescription().toLowerCase().contains(lowerQuery)) {
                    lessonsListBox.getChildren().add(createLessonCard(lesson));
                    foundAny = true;
                }
            }
        }

        if (!foundAny) {
            Label noResultsLabel = new Label("No lessons match your search");
            noResultsLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
            lessonsListBox.getChildren().add(noResultsLabel);
        }
    }

    /**
     * Create a visual card for a lesson
     */
    private HBox createLessonCard(Lesson lesson) {
        HBox card = new HBox(20);
        card.setStyle("-fx-background-color: rgba(15, 52, 96, 0.3); " +
                      "-fx-padding: 20; " +
                      "-fx-background-radius: 12; " +
                      "-fx-border-color: rgba(0, 217, 255, 0.2); " +
                      "-fx-border-width: 1.5; " +
                      "-fx-border-radius: 12;");
        card.setPadding(new Insets(15));

        VBox contentBox = new VBox(8);

        // Title
        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        // Description
        Label descLabel = new Label(lesson.getDescription());
        descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        descLabel.setWrapText(true);

        // Metadata
        HBox metadataBox = new HBox(20);
        Label difficultyLabel = new Label("📊 " + lesson.getDifficulty());
        difficultyLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 12px;");

        Label durationLabel = new Label("⏱️ " + lesson.getDuration() + " min");
        durationLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 12px;");

        metadataBox.getChildren().addAll(difficultyLabel, durationLabel);

        contentBox.getChildren().addAll(titleLabel, descLabel, metadataBox);

        // Buttons
        VBox buttonBox = new VBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");

        Button startButton = new Button("Start Lesson");
        startButton.setStyle("-fx-background-color: linear-gradient(to right, #00ff88, #00d9ff); " +
                           "-fx-text-fill: #1a1a2e; -fx-font-weight: bold;");
        startButton.setMinWidth(120);
        startButton.setOnAction(e -> startLesson(lesson));

        CheckBox completedCheckBox = new CheckBox("Completed");
        completedCheckBox.setStyle("-fx-text-fill: white;");
        completedCheckBox.setSelected(completedLessons.contains(lesson.getTitle()));
        completedCheckBox.setOnAction(e -> toggleLessonCompletion(lesson, completedCheckBox.isSelected()));

        buttonBox.getChildren().addAll(startButton, completedCheckBox);

        card.getChildren().addAll(contentBox, buttonBox);
        HBox.setHgrow(contentBox, javafx.scene.layout.Priority.ALWAYS);

        return card;
    }

    /**
     * Start a lesson
     */
    private void startLesson(Lesson lesson) {
        DialogHelper.showInfo("Lesson Starting",
            "Starting: " + lesson.getTitle() + "\n\n" +
            "In a full implementation, this would open an interactive lesson player.");
        LOGGER.info("Starting lesson: " + lesson.getTitle());
    }

    /**
     * Toggle lesson completion status
     */
    private void toggleLessonCompletion(Lesson lesson, boolean completed) {
        if (completed) {
            completedLessons.add(lesson.getTitle());
            LOGGER.info("Marked lesson as completed: " + lesson.getTitle());
        } else {
            completedLessons.remove(lesson.getTitle());
            LOGGER.info("Unmarked lesson: " + lesson.getTitle());
        }
    }

    /**
     * Return to main menu
     */
    @FXML
    private void handleBackToMenu() {
        try {
            App.setRoot("mainmenu");
            LOGGER.info("Returning to main menu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to main menu", e);
            DialogHelper.showError("Navigation Error", "Could not return to main menu");
        }
    }

    /**
     * Inner class representing a lesson
     */
    private static class Lesson {
        private final String title;
        private final String description;
        private final String difficulty;
        private final int duration;

        public Lesson(String title, String description, String difficulty, int duration) {
            this.title = title;
            this.description = description;
            this.difficulty = difficulty;
            this.duration = duration;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public int getDuration() {
            return duration;
        }
    }
}
