package com.shredforge.calibration;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.shredforge.core.model.AudioDeviceInfo;

import javax.sound.sampled.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Real-time guitar tuner service using pitch detection.
 * Provides continuous frequency feedback for tuning each string.
 */
public final class GuitarTunerService {

    private final Object lock = new Object();
    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private TargetDataLine targetLine;
    private TuningSession session;
    private Consumer<TuningUpdate> updateListener;
    private AudioDeviceInfo audioDevice;
    
    // Audio parameters - matching the working prototype
    private static final float SAMPLE_RATE = 44100f;
    private static final int BUFFER_SIZE = 2048;  // Larger buffer improves low-frequency stability
    private static final int OVERLAP = 1024;
    private static final double MIN_CONFIDENCE = 0.75;  // Higher confidence threshold like prototype
    
    // Tuning parameters
    private static final double DEFAULT_CENTS_TOLERANCE = 5.0; // ±5 cents = in tune
    private static final double IN_TUNE_HOLD_MS = 500; // Must stay in tune for 500ms
    
    private double centsTolerance = DEFAULT_CENTS_TOLERANCE;
    private long inTuneStartTime = 0;
    private boolean wasInTune = false;

    public GuitarTunerService() {
        this.audioDevice = AudioDeviceInfo.systemDefault();
    }

    /**
     * Sets the audio device to use for tuning.
     */
    public void setAudioDevice(AudioDeviceInfo device) {
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Cannot change device while tuning");
            }
            this.audioDevice = device != null ? device : AudioDeviceInfo.systemDefault();
        }
    }

    /**
     * Sets the cents tolerance for considering a string "in tune".
     */
    public void setCentsTolerance(double cents) {
        this.centsTolerance = Math.max(1.0, Math.min(50.0, cents));
    }

    /**
     * Starts a tuning session.
     * @param session the tuning session with strings to tune
     * @param updateListener callback for real-time tuning updates
     */
    public void startTuning(TuningSession session, Consumer<TuningUpdate> updateListener) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(updateListener, "updateListener");
        
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Tuner already running");
            }
            
            this.session = session;
            this.updateListener = updateListener;
            this.inTuneStartTime = 0;
            this.wasInTune = false;
            
            dispatcher = createDispatcher();
            AudioProcessor processor = buildTuningProcessor();
            dispatcher.addAudioProcessor(processor);
            
            audioThread = new Thread(dispatcher, "guitar-tuner");
            audioThread.setDaemon(true);
            audioThread.start();
            
            // Send initial update
            updateListener.accept(new TuningUpdate(
                    session.currentString(),
                    0, 0, 
                    TuningStatus.WAITING,
                    session.currentStringIndex(),
                    session.totalStrings()
            ));
        }
    }

    /**
     * Stops the tuning session.
     */
    public void stopTuning() {
        synchronized (lock) {
            AudioDispatcher d = dispatcher;
            dispatcher = null;
            if (d != null) {
                d.stop();
            }
            Thread t = audioThread;
            audioThread = null;
            if (t != null) {
                try {
                    t.join(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            TargetDataLine line = targetLine;
            targetLine = null;
            if (line != null) {
                line.stop();
                line.close();
            }
            session = null;
            updateListener = null;
        }
    }

    /**
     * Returns true if the tuner is currently running.
     */
    public boolean isRunning() {
        return dispatcher != null;
    }

    /**
     * Advances to the next string in the session.
     * @return true if there are more strings
     */
    public boolean nextString() {
        synchronized (lock) {
            if (session != null) {
                inTuneStartTime = 0;
                wasInTune = false;
                return session.goToNextString();
            }
            return false;
        }
    }

    /**
     * Goes back to the previous string.
     * @return true if moved successfully
     */
    public boolean previousString() {
        synchronized (lock) {
            if (session != null) {
                inTuneStartTime = 0;
                wasInTune = false;
                return session.goToPreviousString();
            }
            return false;
        }
    }

    /**
     * Marks the current string as tuned and advances.
     * @return true if there are more strings
     */
    public boolean confirmAndAdvance() {
        synchronized (lock) {
            if (session != null) {
                inTuneStartTime = 0;
                wasInTune = false;
                return session.markCurrentTunedAndAdvance();
            }
            return false;
        }
    }

    /**
     * Returns the current tuning session.
     */
    public TuningSession getSession() {
        return session;
    }

    private AudioDispatcher createDispatcher() {
        try {
            Mixer.Info mixerInfo = findMixerForDevice(audioDevice);
            
            if (mixerInfo != null) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                
                targetLine = (TargetDataLine) mixer.getLine(info);
                targetLine.open(format, BUFFER_SIZE);
                targetLine.start();
                
                AudioInputStream ais = new AudioInputStream(targetLine);
                TarsosDSPAudioInputStream tarsosIn = new JVMAudioInputStream(ais);
                return new AudioDispatcher(tarsosIn, BUFFER_SIZE, OVERLAP);
            }
            
            return AudioDispatcherFactory.fromDefaultMicrophone(
                    (int) SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to access audio input: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Finds the mixer for a given audio device.
     */
    private static Mixer.Info findMixerForDevice(AudioDeviceInfo device) {
        if (device == null || "default".equals(device.id())) {
            return null;
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(device.name())) {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info targetLineInfo = new Line.Info(TargetDataLine.class);
                if (mixer.isLineSupported(targetLineInfo)) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Builds the pitch detection processor using TarsosDSP's YIN algorithm.
     * This is the same approach used in the working prototype.
     */
    private AudioProcessor buildTuningProcessor() {
        // Use YIN algorithm - same as the working prototype
        PitchDetectionHandler handler = (PitchDetectionResult result, AudioEvent event) -> {
            if (session == null || updateListener == null) {
                return;
            }
            
            float pitchHz = result.getPitch();
            float probability = result.getProbability();
            
            // No pitch detected or low confidence
            if (pitchHz <= 0 || probability < MIN_CONFIDENCE) {
                sendUpdate(0, 0, TuningStatus.WAITING);
                return;
            }
            
            TuningString currentString = session.currentString();
            double targetHz = currentString.targetFrequencyHz();
            
            // Calculate cents deviation: 1200 * log2(detected / target)
            double cents = 1200.0 * Math.log(pitchHz / targetHz) / Math.log(2);
            boolean isInTune = Math.abs(cents) <= centsTolerance;
            
            // Determine status
            TuningStatus status;
            if (isInTune) {
                if (!wasInTune) {
                    inTuneStartTime = System.currentTimeMillis();
                    wasInTune = true;
                }
                
                long inTuneDuration = System.currentTimeMillis() - inTuneStartTime;
                if (inTuneDuration >= IN_TUNE_HOLD_MS) {
                    status = TuningStatus.IN_TUNE;
                } else {
                    status = TuningStatus.ALMOST;
                }
            } else {
                wasInTune = false;
                inTuneStartTime = 0;
                status = cents > 0 ? TuningStatus.SHARP : TuningStatus.FLAT;
            }
            
            sendUpdate(pitchHz, cents, status);
        };
        
        return new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                SAMPLE_RATE,
                BUFFER_SIZE,
                handler
        );
    }

    private void sendUpdate(double frequency, double cents, TuningStatus status) {
        if (updateListener != null && session != null) {
            updateListener.accept(new TuningUpdate(
                    session.currentString(),
                    frequency,
                    cents,
                    status,
                    session.currentStringIndex(),
                    session.totalStrings()
            ));
        }
    }

    /**
     * Represents a real-time tuning update.
     */
    public record TuningUpdate(
            TuningString targetString,
            double detectedFrequency,
            double centsDeviation,
            TuningStatus status,
            int currentStringIndex,
            int totalStrings
    ) {
        public String getDeviationDisplay() {
            if (status == TuningStatus.WAITING) {
                return "Play string " + targetString.stringNumber();
            }
            if (status == TuningStatus.IN_TUNE) {
                return "✓ In Tune!";
            }
            String direction = centsDeviation > 0 ? "+" : "";
            return String.format("%s%.0f cents", direction, centsDeviation);
        }
    }

    /**
     * Tuning status enum.
     */
    public enum TuningStatus {
        WAITING,    // No sound detected
        FLAT,       // Too low
        SHARP,      // Too high
        ALMOST,     // Within tolerance but not held long enough
        IN_TUNE     // Held in tune for required duration
    }
}
