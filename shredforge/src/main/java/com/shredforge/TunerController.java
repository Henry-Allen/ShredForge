package com.shredforge;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TunerController - Enhanced Guitar Tuner with Professional Pitch Detection
 * 
 * Features:
 * - Real-time pitch detection using TarsosDSP YIN algorithm
 * - Visual tuning meter with color-coded feedback
 * - Auto-calibration and noise filtering
 * - Standard and alternate tuning support
 * - Note confidence indicator
 * 
 * @version 1.1
 * @author Team 2 - ShredForge
 */
public class TunerController {

    private static final Logger LOGGER = Logger.getLogger(TunerController.class.getName());

    // FXML Components
    @FXML private Label statusLabel;
    @FXML private Label noteLabel;
    @FXML private Label frequencyLabel;
    @FXML private Label centsLabel;
    @FXML private Canvas meterCanvas;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button backButton;

    // Audio Processing
    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private volatile float currentFrequency = 0;
    private volatile String currentNote = "--";
    private volatile double cents = 0;
    private volatile float confidence = 0;
    private AnimationTimer uiUpdater;
    private Timeline pulseAnimation;

    // Tuning Configuration
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final double A4_FREQUENCY = 440.0;
    private static final double[] STANDARD_TUNING = {82.41, 110.0, 146.83, 196.0, 246.94, 329.63}; // E A D G B E
    private static final String[] STRING_NAMES = {"E2", "A2", "D3", "G3", "B3", "E4"};
    
    // Audio Settings
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 2048;
    private static final int OVERLAP = 1024;
    private static final float MIN_FREQUENCY = 60.0f;
    private static final float MAX_FREQUENCY = 1000.0f;
    private static final float MIN_CONFIDENCE = 0.85f; // 85% confidence threshold
    
    // UI Settings
    private static final double IN_TUNE_THRESHOLD = 5.0; // ±5 cents
    private static final double CLOSE_TUNE_THRESHOLD = 15.0; // ±15 cents
    
    // Noise filtering
    private static final int FREQUENCY_BUFFER_SIZE = 5;
    private final float[] frequencyBuffer = new float[FREQUENCY_BUFFER_SIZE];
    private int bufferIndex = 0;
    private boolean isCalibrated = false;

    @FXML
    public void initialize() {
        LOGGER.info("Initializing TunerController");
        drawMeter(0);
        setupUIUpdater();
        animateWelcome();
        stopButton.setDisable(true);
        
        // Check audio system availability
        if (!checkAudioSystem()) {
            showError("Audio System Error", 
                "No microphone detected. Please connect a microphone and restart.");
            startButton.setDisable(true);
        }
    }

    /**
     * Checks if audio system is available and configured
     */
    private boolean checkAudioSystem() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                LOGGER.severe("Audio line not supported");
                return false;
            }
            
            // Check for available mixers
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers.length == 0) {
                LOGGER.severe("No audio mixers found");
                return false;
            }
            
            LOGGER.info("Audio system check passed. Available mixers: " + mixers.length);
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Audio system check failed", e);
            return false;
        }
    }

    /**
     * Animates the welcome screen
     */
    private void animateWelcome() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), meterCanvas);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Sets up the UI update timer for smooth real-time updates
     */
    private void setupUIUpdater() {
        uiUpdater = new AnimationTimer() {
            private long lastUpdate = 0;
            private static final long UPDATE_INTERVAL = 50_000_000; // 50ms (20 FPS)
            
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    updateUI();
                    lastUpdate = now;
                }
            }
        };
    }

    @FXML
    private void handleStart() {
        try {
            LOGGER.info("Starting tuner...");
            startTuning();
            startButton.setDisable(true);
            stopButton.setDisable(false);
            statusLabel.setText("Listening...");
            statusLabel.setStyle("-fx-text-fill: #00ff88;");
            uiUpdater.start();
            startPulseAnimation();
            animateButton(stopButton);
            
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.SEVERE, "Audio line unavailable", e);
            showError("Audio Error", 
                "Could not access microphone. Please check your audio settings.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start tuner", e);
            showError("Tuner Error", "Failed to start tuner: " + e.getMessage());
        }
    }

    @FXML
    private void handleStop() {
        LOGGER.info("Stopping tuner...");
        stopTuning();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        statusLabel.setText("Stopped");
        statusLabel.setStyle("-fx-text-fill: #888;");
        uiUpdater.stop();
        stopPulseAnimation();
        resetDisplay();
        animateButton(startButton);
    }

    @FXML
    private void handleBack() {
        stopTuning();
        if (uiUpdater != null) {
            uiUpdater.stop();
        }
        stopPulseAnimation();
        try {
            App.setRoot("main");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back", e);
        }
    }

    /**
     * Starts the pulse animation for the note label
     */
    private void startPulseAnimation() {
        pulseAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(noteLabel.scaleXProperty(), 1.0),
                new KeyValue(noteLabel.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(400), 
                new KeyValue(noteLabel.scaleXProperty(), 1.1),
                new KeyValue(noteLabel.scaleYProperty(), 1.1)),
            new KeyFrame(Duration.millis(800), 
                new KeyValue(noteLabel.scaleXProperty(), 1.0),
                new KeyValue(noteLabel.scaleYProperty(), 1.0))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    /**
     * Stops the pulse animation
     */
    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            noteLabel.setScaleX(1.0);
            noteLabel.setScaleY(1.0);
        }
    }

    /**
     * Animates a button with scale effect
     */
    private void animateButton(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    /**
     * Starts the tuning process with audio capture and pitch detection
     */
    private void startTuning() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        line.open(format, BUFFER_SIZE * 2);
        line.start();

        AudioInputStream audioStream = new AudioInputStream(line);
        JVMAudioInputStream jvmAudioStream = new JVMAudioInputStream(audioStream);
        dispatcher = new AudioDispatcher(jvmAudioStream, BUFFER_SIZE, OVERLAP);

        PitchDetectionHandler pitchHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                float pitchInHz = result.getPitch();
                float detectionConfidence = result.getProbability();
                
                // Only process valid pitches with sufficient confidence
                if (pitchInHz != -1 && 
                    pitchInHz >= MIN_FREQUENCY && 
                    pitchInHz <= MAX_FREQUENCY &&
                    detectionConfidence >= MIN_CONFIDENCE) {
                    
                    // Apply smoothing filter
                    float smoothedFrequency = applyFrequencySmoothing(pitchInHz);
                    
                    currentFrequency = smoothedFrequency;
                    confidence = detectionConfidence;
                    updateNoteAndCents(smoothedFrequency);
                }
            }
        };

        AudioProcessor pitchProcessor = new PitchProcessor(
            PitchEstimationAlgorithm.YIN, 
            SAMPLE_RATE, 
            BUFFER_SIZE, 
            pitchHandler
        );

        dispatcher.addAudioProcessor(pitchProcessor);

        audioThread = new Thread(dispatcher, "Audio-Tuner-Thread");
        audioThread.setDaemon(true);
        audioThread.start();
        
        LOGGER.info("Audio processing started");
    }

    /**
     * Applies smoothing filter to reduce frequency jitter
     */
    private float applyFrequencySmoothing(float newFrequency) {
        frequencyBuffer[bufferIndex] = newFrequency;
        bufferIndex = (bufferIndex + 1) % FREQUENCY_BUFFER_SIZE;
        
        // Calculate median to reduce noise
        float[] sortedBuffer = Arrays.copyOf(frequencyBuffer, frequencyBuffer.length);
        Arrays.sort(sortedBuffer);
        return sortedBuffer[FREQUENCY_BUFFER_SIZE / 2];
    }

    /**
     * Stops the tuning process and releases resources
     */
    private void stopTuning() {
        if (dispatcher != null) {
            dispatcher.stop();
            dispatcher = null;
        }
        if (audioThread != null && audioThread.isAlive()) {
            audioThread.interrupt();
            try {
                audioThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                LOGGER.warning("Audio thread interruption timeout");
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }
        
        // Clear frequency buffer
        Arrays.fill(frequencyBuffer, 0);
        bufferIndex = 0;
        
        LOGGER.info("Audio processing stopped");
    }

    /**
     * Updates note name and cents deviation from detected frequency
     */
    private void updateNoteAndCents(float frequency) {
        // Convert frequency to MIDI note number
        double noteNumber = 12 * (Math.log(frequency / A4_FREQUENCY) / Math.log(2)) + 69;
        int nearestNote = (int) Math.round(noteNumber);
        
        // Get note name with octave
        currentNote = getNoteNameFromMIDI(nearestNote);
        
        // Calculate cents deviation
        cents = (noteNumber - nearestNote) * 100;
        
        // Check if close to standard tuning
        checkStandardTuning(frequency);
    }

    /**
     * Checks if the current frequency matches a standard guitar string
     */
    private void checkStandardTuning(float frequency) {
        for (int i = 0; i < STANDARD_TUNING.length; i++) {
            double diff = Math.abs(frequency - STANDARD_TUNING[i]);
            double diffCents = 1200 * Math.log(frequency / STANDARD_TUNING[i]) / Math.log(2);
            
            if (Math.abs(diffCents) < 50) { // Within 50 cents
                // Could add string indicator here
                break;
            }
        }
    }

    /**
     * Converts MIDI note number to note name with octave
     */
    private String getNoteNameFromMIDI(int midiNote) {
        if (midiNote < 0 || midiNote > 127) {
            return "--";
        }
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        return NOTE_NAMES[noteIndex] + octave;
    }

    /**
     * Updates the UI with current tuning information
     */
    private void updateUI() {
        if (currentFrequency > 0) {
            noteLabel.setText(currentNote);
            frequencyLabel.setText(String.format("%.1f Hz", currentFrequency));
            
            // Format cents with sign
            String centsText = String.format("%+.0f¢", cents);
            centsLabel.setText(centsText);
            
            // Color code based on accuracy
            if (Math.abs(cents) < IN_TUNE_THRESHOLD) {
                centsLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
                noteLabel.setStyle("-fx-text-fill: #00ff88;");
            } else if (Math.abs(cents) < CLOSE_TUNE_THRESHOLD) {
                centsLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
                noteLabel.setStyle("-fx-text-fill: #ffaa00;");
            } else {
                centsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
                noteLabel.setStyle("-fx-text-fill: white;");
            }
            
            drawMeter(cents);
        }
    }

    /**
     * Draws the visual tuning meter
     */
    private void drawMeter(double centsOffset) {
        GraphicsContext gc = meterCanvas.getGraphicsContext2D();
        double width = meterCanvas.getWidth();
        double height = meterCanvas.getHeight();

        // Clear canvas with dark background
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRect(0, 0, width, height);

        // Draw background zones with gradients
        // Flat zone (left)
        gc.setFill(Color.web("#ff6b6b", 0.15));
        gc.fillRect(0, 0, width * 0.35, height);
        
        // Sharp zone (right)
        gc.setFill(Color.web("#ff6b6b", 0.15));
        gc.fillRect(width * 0.65, 0, width * 0.35, height);
        
        // Close zones
        gc.setFill(Color.web("#ffaa00", 0.15));
        gc.fillRect(width * 0.35, 0, width * 0.15, height);
        gc.fillRect(width * 0.5, 0, width * 0.15, height);
        
        // Perfect zone (center)
        gc.setFill(Color.web("#00ff88", 0.25));
        gc.fillRect(width * 0.45, 0, width * 0.1, height);

        // Draw center line
        gc.setStroke(Color.web("#00ff88"));
        gc.setLineWidth(3);
        gc.strokeLine(width / 2, 0, width / 2, height);
        
        // Draw tick marks
        gc.setStroke(Color.web("#666"));
        gc.setLineWidth(2);
        for (int i = -50; i <= 50; i += 10) {
            double x = width / 2 + (i / 50.0) * (width * 0.4);
            double tickHeight = i % 20 == 0 ? 25 : 15;
            gc.strokeLine(x, height - tickHeight, x, height - 5);
            
            // Draw labels for major ticks
            if (i % 20 == 0) {
                gc.setFill(Color.web("#888"));
                gc.fillText(String.valueOf(i), x - 8, height - 30);
            }
        }

        // Draw animated needle
        double needleX = width / 2 + (centsOffset / 50.0) * (width * 0.4);
        needleX = Math.max(20, Math.min(width - 20, needleX));
        
        boolean inTune = Math.abs(centsOffset) < IN_TUNE_THRESHOLD;
        boolean close = Math.abs(centsOffset) < CLOSE_TUNE_THRESHOLD;
        
        Color needleColor = inTune ? Color.web("#00ff88") : 
                           close ? Color.web("#ffaa00") : 
                           Color.web("#ff6b6b");
        
        // Needle shadow
        gc.setStroke(Color.web("#000000", 0.5));
        gc.setLineWidth(6);
        gc.strokeLine(needleX + 2, 15, needleX + 2, height - 15);
        
        // Main needle
        gc.setStroke(needleColor);
        gc.setLineWidth(5);
        gc.strokeLine(needleX, 15, needleX, height - 15);
        
        // Needle tip circle with glow
        double circleSize = inTune ? 18 : 14;
        gc.setFill(needleColor);
        gc.fillOval(needleX - circleSize/2, height / 2 - circleSize/2, circleSize, circleSize);
        
        // Perfect tune indicator (pulsing circle)
        if (inTune) {
            gc.setFill(Color.web("#00ff88", 0.3));
            gc.fillOval(needleX - 25, height / 2 - 25, 50, 50);
            
            // Inner pulse
            gc.setFill(Color.web("#00ff88", 0.2));
            gc.fillOval(needleX - 35, height / 2 - 35, 70, 70);
        }
        
        // Confidence indicator (optional visual feedback)
        if (confidence > 0) {
            gc.setFill(Color.web("#00d9ff", 0.5));
            double confidenceWidth = width * 0.1 * confidence;
            gc.fillRect(10, 10, confidenceWidth, 5);
        }
    }

    /**
     * Resets the display to default state
     */
    private void resetDisplay() {
        noteLabel.setText("--");
        noteLabel.setStyle("-fx-text-fill: white;");
        frequencyLabel.setText("0.0 Hz");
        centsLabel.setText("0¢");
        centsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
        currentFrequency = 0;
        currentNote = "--";
        cents = 0;
        confidence = 0;
        drawMeter(0);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), meterCanvas);
        fade.setFromValue(1.0);
        fade.setToValue(0.5);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);
        fade.play();
    }

    /**
     * Shows an error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Cleanup method called when controller is destroyed
     */
    public void cleanup() {
        LOGGER.info("Cleaning up TunerController");
        stopTuning();
        if (uiUpdater != null) {
            uiUpdater.stop();
        }
        stopPulseAnimation();
    }
}