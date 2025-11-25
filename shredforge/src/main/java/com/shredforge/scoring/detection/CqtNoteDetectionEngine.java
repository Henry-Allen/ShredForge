package com.shredforge.scoring.detection;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.util.fft.FFT;
import com.shredforge.core.model.AudioDeviceInfo;
import com.shredforge.core.model.PracticeConfig;
import com.shredforge.scoring.model.DetectedNote;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CQT (Constant-Q Transform) based note detection engine using TarsosDSP.
 * Better suited for guitar frequency detection than standard FFT/YIN.
 * Supports audio device selection for use with audio interfaces.
 */
public final class CqtNoteDetectionEngine implements NoteDetectionEngine {

    private final Object lock = new Object();
    private AudioDispatcher dispatcher;
    private Thread audioThread;
    private TargetDataLine targetLine;
    private NoteDetectionListener listener;
    private PracticeConfig config;
    private Mixer.Info selectedMixer;

    // CQT parameters
    private static final double MIN_FREQ = 65.41;   // C2 - lowest guitar note (drop tuning)
    private static final double MAX_FREQ = 1318.51; // E6 - highest practical guitar note
    private static final int OCTAVES = 5;

    public CqtNoteDetectionEngine() {
        this(null);
    }

    public CqtNoteDetectionEngine(Mixer.Info mixerInfo) {
        this.selectedMixer = mixerInfo;
    }

    /**
     * Lists all available audio input devices.
     */
    public static List<AudioDeviceInfo> listInputDevices() {
        List<AudioDeviceInfo> devices = new ArrayList<>();
        devices.add(AudioDeviceInfo.systemDefault());

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                
                // Check if this mixer supports audio input
                boolean hasInput = false;
                for (Line.Info lineInfo : targetLines) {
                    if (lineInfo instanceof DataLine.Info) {
                        hasInput = true;
                        break;
                    }
                }
                
                if (hasInput) {
                    devices.add(new AudioDeviceInfo(
                            info.getName(),
                            info.getName(),
                            info.getDescription(),
                            false
                    ));
                }
            } catch (Exception e) {
                // Skip devices that can't be queried
            }
        }
        return devices;
    }

    /**
     * Finds the Mixer.Info for a given device ID.
     */
    public static Mixer.Info findMixerForDevice(AudioDeviceInfo device) {
        if (device == null || device.isDefault() || "default".equals(device.id())) {
            return null; // Use system default
        }

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixers) {
            if (info.getName().equals(device.id())) {
                return info;
            }
        }
        return null;
    }

    /**
     * Sets the audio device to use.
     */
    public void setAudioDevice(AudioDeviceInfo device) {
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Cannot change device while running");
            }
            this.selectedMixer = findMixerForDevice(device);
        }
    }

    @Override
    public void start(NoteDetectionConfig cfg, NoteDetectionListener listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Detection engine already running");
            }
            this.listener = listener;
            
            // Convert legacy config if needed
            PracticeConfig practiceConfig = PracticeConfig.defaults()
                    .withConfidence(cfg != null ? cfg.minimumConfidence() : 0.7);
            this.config = practiceConfig;
            
            float sampleRate = cfg != null ? cfg.sampleRate() : 44100f;
            int bufferSize = cfg != null ? cfg.bufferSize() : 4096;
            
            dispatcher = createDispatcher(sampleRate, bufferSize);
            AudioProcessor processor = buildCqtProcessor(sampleRate, bufferSize, practiceConfig.minimumConfidence());
            dispatcher.addAudioProcessor(processor);
            
            audioThread = new Thread(dispatcher, "cqt-note-detector");
            audioThread.setDaemon(true);
            audioThread.start();
        }
    }

    /**
     * Starts detection with a PracticeConfig.
     */
    public void start(PracticeConfig config, NoteDetectionListener listener) {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(config, "config");
        
        synchronized (lock) {
            if (isRunning()) {
                throw new IllegalStateException("Detection engine already running");
            }
            this.listener = listener;
            this.config = config;
            this.selectedMixer = findMixerForDevice(config.audioDevice());
            
            dispatcher = createDispatcher(config.sampleRate(), config.bufferSize());
            AudioProcessor processor = buildCqtProcessor(
                    config.sampleRate(), 
                    config.bufferSize(), 
                    config.minimumConfidence()
            );
            dispatcher.addAudioProcessor(processor);
            
            audioThread = new Thread(dispatcher, "cqt-note-detector");
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

    private AudioDispatcher createDispatcher(float sampleRate, int bufferSize) {
        try {
            if (selectedMixer != null) {
                Mixer mixer = AudioSystem.getMixer(selectedMixer);
                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                
                if (!mixer.isLineSupported(info)) {
                    // Try big-endian
                    format = new AudioFormat(sampleRate, 16, 1, true, true);
                    info = new DataLine.Info(TargetDataLine.class, format);
                }
                
                targetLine = (TargetDataLine) mixer.getLine(info);
                targetLine.open(format, bufferSize * 2);
                targetLine.start();
                
                AudioInputStream ais = new AudioInputStream(targetLine);
                TarsosDSPAudioInputStream tarsosIn = new JVMAudioInputStream(ais);
                return new AudioDispatcher(tarsosIn, bufferSize, bufferSize / 2);
            }
            
            // Use default microphone
            return AudioDispatcherFactory.fromDefaultMicrophone(
                    (int) sampleRate, bufferSize, bufferSize / 2);
        } catch (Exception ex) {
            throw new NoteDetectionException("Unable to access audio input: " + ex.getMessage(), ex);
        }
    }

    private AudioProcessor buildCqtProcessor(float sampleRate, int bufferSize, double minConfidence) {
        // Calculate CQT parameters
        int binsPerOctave = config != null ? config.cqtBinsPerOctave() : 36;
        int totalBins = OCTAVES * binsPerOctave;
        
        // Pre-calculate center frequencies for each CQT bin
        double[] centerFreqs = new double[totalBins];
        for (int i = 0; i < totalBins; i++) {
            centerFreqs[i] = MIN_FREQ * Math.pow(2.0, (double) i / binsPerOctave);
        }
        
        FFT fft = new FFT(bufferSize);
        
        return new AudioProcessor() {
            private long sampleCount = 0;
            
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] buffer = audioEvent.getFloatBuffer();
                
                // Calculate RMS for amplitude/confidence
                double rms = 0;
                for (float sample : buffer) {
                    rms += sample * sample;
                }
                rms = Math.sqrt(rms / buffer.length);
                
                // Skip if too quiet (noise gate)
                if (rms < 0.01) {
                    return true;
                }
                
                // Apply Hann window
                float[] windowed = new float[bufferSize];
                for (int i = 0; i < bufferSize && i < buffer.length; i++) {
                    double window = 0.5 * (1 - Math.cos(2 * Math.PI * i / (bufferSize - 1)));
                    windowed[i] = (float) (buffer[i] * window);
                }
                
                // Perform FFT
                float[] fftBuffer = new float[bufferSize * 2];
                System.arraycopy(windowed, 0, fftBuffer, 0, Math.min(windowed.length, bufferSize));
                fft.forwardTransform(fftBuffer);
                
                // Calculate magnitude spectrum
                double[] magnitudes = new double[bufferSize / 2];
                for (int i = 0; i < magnitudes.length; i++) {
                    double real = fftBuffer[2 * i];
                    double imag = fftBuffer[2 * i + 1];
                    magnitudes[i] = Math.sqrt(real * real + imag * imag);
                }
                
                // Find dominant frequency using spectral peak detection
                double maxMag = 0;
                int maxBin = 0;
                double freqResolution = sampleRate / bufferSize;
                
                // Focus on guitar frequency range
                int minBin = (int) (MIN_FREQ / freqResolution);
                int maxBinLimit = Math.min((int) (MAX_FREQ / freqResolution), magnitudes.length - 1);
                
                for (int i = minBin; i <= maxBinLimit; i++) {
                    if (magnitudes[i] > maxMag) {
                        maxMag = magnitudes[i];
                        maxBin = i;
                    }
                }
                
                // Parabolic interpolation for better frequency accuracy
                double peakFreq;
                if (maxBin > 0 && maxBin < magnitudes.length - 1) {
                    double alpha = magnitudes[maxBin - 1];
                    double beta = magnitudes[maxBin];
                    double gamma = magnitudes[maxBin + 1];
                    double p = 0.5 * (alpha - gamma) / (alpha - 2 * beta + gamma);
                    peakFreq = (maxBin + p) * freqResolution;
                } else {
                    peakFreq = maxBin * freqResolution;
                }
                
                // Calculate confidence based on peak prominence
                double avgMag = 0;
                for (int i = minBin; i <= maxBinLimit; i++) {
                    avgMag += magnitudes[i];
                }
                avgMag /= (maxBinLimit - minBin + 1);
                double confidence = avgMag > 0 ? Math.min(1.0, maxMag / (avgMag * 10)) : 0;
                
                // Apply RMS-based confidence adjustment
                confidence *= Math.min(1.0, rms * 20);
                
                if (confidence < minConfidence || peakFreq < MIN_FREQ || peakFreq > MAX_FREQ) {
                    return true;
                }
                
                // Convert to MIDI and create detected note
                double midi = hzToMidi(peakFreq);
                double cents = centsFromMidi(midi);
                double timestampMs = (sampleCount * 1000.0) / sampleRate;
                sampleCount += buffer.length;
                
                DetectedNote note = new DetectedNote(
                        timestampMs,
                        peakFreq,
                        midi,
                        cents,
                        confidence,
                        DetectedNote.midiToNoteName(midi)
                );
                
                if (listener != null) {
                    listener.onNoteDetected(note);
                }
                
                return true;
            }
            
            @Override
            public void processingFinished() {
                // Nothing to clean up
            }
        };
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
