package com.shredforge;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class LessonsController {

    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> lessonList;
    @FXML private TextArea lessonPreview;
    @FXML private Button startLessonButton;
    @FXML private Button backButton;
    @FXML private Label categoryLabel;
    @FXML private ProgressBar categoryProgress;

    private ObservableList<String> categories;
    private ObservableList<String> currentLessons;

    @FXML
    public void initialize() {
        setupCategories();
        setupEventHandlers();
        categoryList.getSelectionModel().selectFirst();
        loadLessonsForCategory(0);
    }

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
    }

    private void setupEventHandlers() {
        categoryList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadLessonsForCategory(newVal.intValue());
                }
            }
        );

        lessonList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showLessonPreview(newVal.intValue());
                }
            }
        );
    }

    private void loadLessonsForCategory(int categoryIndex) {
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
                categoryProgress.setProgress(0.5);
                categoryLabel.setText("Beginner Fundamentals - 3/6 Complete");
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
                categoryProgress.setProgress(0.16);
                categoryLabel.setText("Chord Mastery - 1/6 Complete");
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
                categoryProgress.setProgress(0.0);
                categoryLabel.setText("Speed & Technique - 0/6 Complete");
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
                categoryProgress.setProgress(0.0);
                categoryLabel.setText("Music Theory - 0/6 Complete");
                break;

            case 4: // Songs & Repertoire
                currentLessons = FXCollections.observableArrayList(
                    "Song 1: Smoke on the Water",
                    "Song 2: Seven Nation Army",
                    "Song 3: Wonderwall",
                    "Song 4: Sweet Child O' Mine",
                    "Song 5: Blackbird",
                    "Song 6: Stairway to Heaven"
                );
                categoryProgress.setProgress(0.0);
                categoryLabel.setText("Songs & Repertoire - 0/6 Complete");
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
                categoryProgress.setProgress(0.0);
                categoryLabel.setText("Advanced Shredding - 0/6 Complete");
                break;
        }

        lessonList.setItems(currentLessons);
        lessonList.getSelectionModel().selectFirst();
    }

    private void showLessonPreview(int lessonIndex) {
        String preview = "Lesson Preview\n\n";
        
        int categoryIndex = categoryList.getSelectionModel().getSelectedIndex();
        
        // Generate different preview based on category and lesson
        if (categoryIndex == 0 && lessonIndex == 3) {
            preview += "Strumming Patterns\n\n" +
                      "Duration: 15 minutes\n" +
                      "Difficulty: Beginner\n\n" +
                      "What You'll Learn:\n" +
                      "• Basic down strumming pattern\n" +
                      "• Adding upstrokes\n" +
                      "• Common 4/4 patterns\n" +
                      "• Maintaining steady rhythm\n\n" +
                      "Required Skills:\n" +
                      "• Ability to hold guitar correctly\n" +
                      "• Knowledge of at least 2 chords\n\n" +
                      "This lesson introduces fundamental strumming patterns " +
                      "that you'll use in countless songs. We'll start with simple " +
                      "down strums and gradually add upstrokes to create more " +
                      "dynamic rhythm patterns.";
        } else if (categoryIndex == 2 && lessonIndex == 0) {
            preview += "Alternate Picking Basics\n\n" +
                      "Duration: 20 minutes\n" +
                      "Difficulty: Intermediate\n\n" +
                      "What You'll Learn:\n" +
                      "• Proper pick grip and angle\n" +
                      "• Down-up motion mechanics\n" +
                      "• Chromatic exercises\n" +
                      "• Building speed gradually\n\n" +
                      "Required Skills:\n" +
                      "• Basic fretting hand technique\n" +
                      "• Ability to play single notes\n\n" +
                      "Alternate picking is essential for speed and accuracy. " +
                      "This lesson covers the fundamentals that will serve as " +
                      "the foundation for all fast playing techniques.";
        } else {
            preview += currentLessons.get(lessonIndex) + "\n\n" +
                      "Duration: 15-20 minutes\n" +
                      "Difficulty: Varies\n\n" +
                      "This lesson contains structured exercises and theory " +
                      "to help you progress on your guitar journey.\n\n" +
                      "Select 'Start Lesson' to begin!";
        }

        lessonPreview.setText(preview);
        
        // Enable start button only if lesson is not completed
        String selectedLesson = lessonList.getSelectionModel().getSelectedItem();
        startLessonButton.setDisable(selectedLesson != null && selectedLesson.contains("✓"));
    }

    @FXML
    private void handleStartLesson() {
        String lesson = lessonList.getSelectionModel().getSelectedItem();
        if (lesson != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Start Lesson");
            alert.setHeaderText("Starting: " + lesson);
            alert.setContentText(
                "This lesson interface would contain:\n" +
                "• Video instruction\n" +
                "• Interactive tablature\n" +
                "• Real-time feedback\n" +
                "• Progress tracking\n\n" +
                "Full lesson UI coming soon!"
            );
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
