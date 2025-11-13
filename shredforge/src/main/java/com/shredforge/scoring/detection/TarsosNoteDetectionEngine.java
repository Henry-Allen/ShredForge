package com.shredforge.scoring.detection;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import com.shredforge.scoring.model.DetectedNote;
import java.util.Objects;
import javax.sound.sampled.*;

/**
 * TarsosDSP-backed implementation that listens to a microphone and emits detected notes.
 */
public final class TarsosNoteDetectionEngine implements NoteDetectionEngine {

    private final Mixer.Info mixerInfo;
    private final Object lock = new Object();

    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private TargetDataLine targetLine;
    private NoteDetectionListener listener;
    private NoteDetectionConfig config = NoteDetectionConfig.defaults();

    public TarsosNoteDetectionEngine() {
        this(null);
    }

    public TarsosNoteDetectionEngine(Mixer.Info mixerInfo) {
        this.mixerInfo = mixerInfo;
    }

    @Override
    public void start(NoteDetectionConfig cfg, NoteDetectionListener listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Note detection engine already running.");
            }
            this.listener = listener;
            this.config = cfg == null ? NoteDetectionConfig.defaults() : cfg;
            dispatcher = createDispatcher(this.config);
            AudioProcessor processor = buildPitchProcessor(this.config);
            dispatcher.addAudioProcessor(processor);
            audioThread = new Thread(dispatcher, "tarsos-note-detector");
            audioThread.setDaemon(true);
            audioThread.start();
        }
    }

    @Override
    public void stop() {
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
        }
    }

    @Override
    public boolean isRunning() {
        return dispatcher != null;
    }

    private AudioDispatcher createDispatcher(NoteDetectionConfig cfg) {
        try {
            if (mixerInfo != null) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                AudioFormat format = new AudioFormat(cfg.sampleRate(), 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                targetLine = (TargetDataLine) mixer.getLine(info);
                targetLine.open(format, cfg.bufferSize());
                targetLine.start();
                AudioInputStream ais = new AudioInputStream(targetLine);
                TarsosDSPAudioInputStream tarsosIn = new JVMAudioInputStream(ais);
                return new AudioDispatcher(tarsosIn, cfg.bufferSize(), cfg.overlap());
            }
            return AudioDispatcherFactory.fromDefaultMicrophone(
                    (int) cfg.sampleRate(), cfg.bufferSize(), cfg.overlap());
        } catch (Exception ex) {
            throw new NoteDetectionException("Unable to access audio input: " + ex.getMessage(), ex);
        }
    }

    private AudioProcessor buildPitchProcessor(NoteDetectionConfig cfg) {
        PitchDetectionHandler handler = (PitchDetectionResult result, AudioEvent event) -> {
            if (result == null || listener == null) {
                return;
            }
            float pitch = result.getPitch();
            double probability = result.getProbability();
            if (probability < cfg.minimumConfidence()) {
                return;
            }
            double timestampMs = event.getTimeStamp() * 1000.0;
            double midi = hzToMidi(pitch);
            double cents = centsFromMidi(midi);
            DetectedNote note = new DetectedNote(
                    timestampMs,
                    pitch,
                    midi,
                    cents,
                    probability,
                    DetectedNote.midiToNoteName(midi));
            listener.onNoteDetected(note);
        };
        return new PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                cfg.sampleRate(),
                cfg.bufferSize(),
                handler);
    }

    private static double hzToMidi(double hz) {
        if (hz <= 0) {
            return Double.NaN;
        }
        return 69 + 12 * (Math.log(hz / 440.0) / Math.log(2));
    }

    private static double centsFromMidi(double midi) {
        if (Double.isNaN(midi)) {
            return Double.NaN;
        }
        return (midi - Math.round(midi)) * 100.0;
    }
}
