package com.shredforge.input;

import javax.sound.sampled.*;
import java.util.logging.Logger;

/**
 * Handles audio input from guitar through Java Sound API.
 * Captures audio stream from the selected input device.
 */
public class AudioInput {
    private static final Logger LOGGER = Logger.getLogger(AudioInput.class.getName());
    private static final float SAMPLE_RATE = 44100.0f;  // 44.1 kHz
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;  // Mono
    private static final int BUFFER_SIZE = 2048;

    private TargetDataLine targetDataLine;
    private AudioFormat format;
    private boolean isStreamOpen;

    public AudioInput() {
        this.format = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            true,  // signed
            true   // big endian
        );
        this.isStreamOpen = false;
    }

    /**
     * Open audio stream from default input device
     */
    public boolean openStream() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                LOGGER.warning("Audio line not supported");
                return false;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format, BUFFER_SIZE);
            targetDataLine.start();

            isStreamOpen = true;
            LOGGER.info("Audio stream opened successfully");
            return true;

        } catch (LineUnavailableException e) {
            LOGGER.severe("Failed to open audio stream: " + e.getMessage());
            return false;
        }
    }

    /**
     * Open stream from specific device
     */
    public boolean openStream(String deviceName) {
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();

            for (Mixer.Info mixerInfo : mixers) {
                if (mixerInfo.getName().contains(deviceName)) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                    if (mixer.isLineSupported(info)) {
                        targetDataLine = (TargetDataLine) mixer.getLine(info);
                        targetDataLine.open(format, BUFFER_SIZE);
                        targetDataLine.start();

                        isStreamOpen = true;
                        LOGGER.info("Audio stream opened from device: " + deviceName);
                        return true;
                    }
                }
            }

            LOGGER.warning("Device not found: " + deviceName + ", using default");
            return openStream();

        } catch (Exception e) {
            LOGGER.severe("Failed to open audio stream from device: " + e.getMessage());
            return openStream();
        }
    }

    /**
     * Read audio data from the stream
     * @return Array of audio samples as floats (-1.0 to 1.0)
     */
    public float[] readAudioData() {
        if (!isStreamOpen || targetDataLine == null) {
            return new float[0];
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

        if (bytesRead <= 0) {
            return new float[0];
        }

        // Convert bytes to floats
        float[] audioData = new float[bytesRead / 2];  // 2 bytes per sample (16-bit)

        for (int i = 0; i < audioData.length; i++) {
            // Combine two bytes into one 16-bit sample (big endian)
            int sample = ((buffer[i * 2] & 0xFF) << 8) | (buffer[i * 2 + 1] & 0xFF);

            // Convert to signed
            if (sample > 32767) {
                sample -= 65536;
            }

            // Normalize to -1.0 to 1.0
            audioData[i] = sample / 32768.0f;
        }

        return audioData;
    }

    /**
     * Close audio stream
     */
    public void closeStream() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            isStreamOpen = false;
            LOGGER.info("Audio stream closed");
        }
    }

    /**
     * Get available audio input devices
     */
    public static String[] getAvailableDevices() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        return java.util.Arrays.stream(mixers)
            .map(Mixer.Info::getName)
            .toArray(String[]::new);
    }

    // Getters
    public boolean isStreamOpen() {
        return isStreamOpen;
    }

    public float getSampleRate() {
        return SAMPLE_RATE;
    }

    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    public AudioFormat getFormat() {
        return format;
    }
}
