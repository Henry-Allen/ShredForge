package com.shredforge;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import javax.sound.sampled.*;

public class TunerController {

    @FXML private Label statusLabel;
    @FXML private Label noteLabel;
    @FXML private Label frequencyLabel;
    @FXML private Label centsLabel;
    @FXML private Canvas meterCanvas;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button backButton;

    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private volatile float currentFrequency = 0;
    private volatile String currentNote = "--";
    private volatile double cents = 0;
    private AnimationTimer uiUpdater;

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final double A4_FREQUENCY = 440.0;

    @FXML
    public void initialize() {
        drawMeter(0);
        setupUIUpdater();
    }

    private void setupUIUpdater() {
        uiUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateUI();
            }
        };
    }

    @FXML
    private void handleStart() {
        try {
            startTuning();
            startButton.setDisable(true);
            stopButton.setDisable(false);
            statusLabel.setText("Listening...");
            uiUpdater.start();
        } catch (Exception e) {
            showError("Failed to start tuner: " + e.getMessage());
        }
    }

    @FXML
    private void handleStop() {
        stopTuning();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        statusLabel.setText("Stopped");
        uiUpdater.stop();
        resetDisplay();
    }

    @FXML
    private void handleBack() {
        stopTuning();
        if (uiUpdater != null) {
            uiUpdater.stop();
        }
        try {
            App.setRoot("main");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTuning() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        line.open(format, 4096);
        line.start();

        AudioInputStream audioStream = new AudioInputStream(line);
        JVMAudioInputStream jvmAudioStream = new JVMAudioInputStream(audioStream);
        dispatcher = new AudioDispatcher(jvmAudioStream, 2048, 1024);

        PitchDetectionHandler pitchHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                float pitchInHz = result.getPitch();
                if (pitchInHz != -1 && pitchInHz > 60 && pitchInHz < 1000) {
                    currentFrequency = pitchInHz;
                    updateNoteAndCents(pitchInHz);
                }
            }
        };

        AudioProcessor pitchProcessor = new PitchProcessor(
            PitchEstimationAlgorithm.YIN, 
            44100, 
            2048, 
            pitchHandler
        );

        dispatcher.addAudioProcessor(pitchProcessor);

        audioThread = new Thread(dispatcher, "Audio Dispatcher");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void stopTuning() {
        if (dispatcher != null) {
            dispatcher.stop();
        }
        if (audioThread != null && audioThread.isAlive()) {
            audioThread.interrupt();
        }
    }

    private void updateNoteAndCents(float frequency) {
        double noteNumber = 12 * (Math.log(frequency / A4_FREQUENCY) / Math.log(2)) + 69;
        int nearestNote = (int) Math.round(noteNumber);
        currentNote = getNoteNameFromMIDI(nearestNote);
        cents = (noteNumber - nearestNote) * 100;
    }

    private String getNoteNameFromMIDI(int midiNote) {
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        return NOTE_NAMES[noteIndex] + octave;
    }

    private void updateUI() {
        if (currentFrequency > 0) {
            noteLabel.setText(currentNote);
            frequencyLabel.setText(String.format("%.1f Hz", currentFrequency));
            centsLabel.setText(String.format("%.0f¢", cents));
            drawMeter(cents);
        }
    }

    private void drawMeter(double centsOffset) {
        GraphicsContext gc = meterCanvas.getGraphicsContext2D();
        double width = meterCanvas.getWidth();
        double height = meterCanvas.getHeight();

        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.web("#ff6b6b", 0.2));
        gc.fillRect(0, 0, width * 0.4, height);
        gc.fillRect(width * 0.6, 0, width * 0.4, height);
        
        gc.setFill(Color.web("#00ff88", 0.2));
        gc.fillRect(width * 0.4, 0, width * 0.2, height);

        gc.setStroke(Color.web("#00ff88"));
        gc.setLineWidth(2);
        gc.strokeLine(width / 2, 0, width / 2, height);

        gc.setStroke(Color.web("#888"));
        gc.setLineWidth(1);
        for (int i = -50; i <= 50; i += 10) {
            double x = width / 2 + (i / 50.0) * (width * 0.4);
            gc.strokeLine(x, height - 20, x, height - 10);
        }

        double needleX = width / 2 + (centsOffset / 50.0) * (width * 0.4);
        needleX = Math.max(0, Math.min(width, needleX));
        
        Color needleColor = Math.abs(centsOffset) < 5 ? Color.web("#00ff88") : Color.web("#ff6b6b");
        gc.setStroke(needleColor);
        gc.setLineWidth(4);
        gc.strokeLine(needleX, 10, needleX, height - 10);
        
        gc.setFill(needleColor);
        gc.fillOval(needleX - 8, height / 2 - 8, 16, 16);
    }

    private void resetDisplay() {
        noteLabel.setText("--");
        frequencyLabel.setText("0.0 Hz");
        centsLabel.setText("0¢");
        currentFrequency = 0;
        currentNote = "--";
        cents = 0;
        drawMeter(0);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
