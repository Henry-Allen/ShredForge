package com.shredforge.input;

import com.shredforge.util.AudioBufferPool;

import javax.sound.sampled.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles audio input from guitar through Java Sound API.
 * Captures audio stream from the selected input device with robust error handling.
 */
public class AudioInput {
    private static final Logger LOGGER = Logger.getLogger(AudioInput.class.getName());
    private static final float SAMPLE_RATE = 44100.0f;  // 44.1 kHz
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;  // Mono
    private static final int BUFFER_SIZE = 2048;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private TargetDataLine targetDataLine;
    private AudioFormat format;
    private boolean isStreamOpen;
    private AudioBufferPool bufferPool;
    private volatile boolean hasAudioDevice;
    private String lastError;

    public AudioInput() {
        this.format = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            true,  // signed
            true   // big endian
        );
        this.isStreamOpen = false;
        this.bufferPool = new AudioBufferPool(BUFFER_SIZE / 2); // float buffer size
        this.hasAudioDevice = checkAudioDeviceAvailability();
        this.lastError = null;
    }

    /**
     * Check if audio devices are available
     */
    private boolean checkAudioDeviceAvailability() {
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers == null || mixers.length == 0) {
                LOGGER.warning("No audio mixers found");
                return false;
            }

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking audio device availability", e);
            return false;
        }
    }

    /**
     * Open audio stream from default input device with retry logic
     */
    public boolean openStream() {
        if (!hasAudioDevice) {
            lastError = "No audio devices available";
            LOGGER.severe(lastError);
            return false;
        }

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    lastError = "Audio line format not supported";
                    LOGGER.warning(lastError);

                    // Try with different format
                    if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                        format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);
                        continue;
                    }
                    return false;
                }

                targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                targetDataLine.open(format, BUFFER_SIZE);
                targetDataLine.start();

                isStreamOpen = true;
                lastError = null;
                LOGGER.info("Audio stream opened successfully on attempt " + (attempt + 1));
                return true;

            } catch (LineUnavailableException e) {
                lastError = "Audio line unavailable: " + e.getMessage();
                LOGGER.warning("Attempt " + (attempt + 1) + " failed: " + lastError);

                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    try {
                        Thread.sleep(500); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            } catch (Exception e) {
                lastError = "Unexpected error opening audio stream: " + e.getMessage();
                LOGGER.log(Level.SEVERE, lastError, e);
                return false;
            }
        }

        LOGGER.severe("Failed to open audio stream after " + MAX_RETRY_ATTEMPTS + " attempts");
        return false;
    }

    /**
     * Open stream from specific device with fallback to default
     */
    public boolean openStream(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            LOGGER.warning("Invalid device name, using default");
            return openStream();
        }

        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers == null || mixers.length == 0) {
                lastError = "No audio mixers available";
                LOGGER.warning(lastError + ", cannot open device: " + deviceName);
                return false;
            }

            for (Mixer.Info mixerInfo : mixers) {
                try {
                    if (mixerInfo.getName().contains(deviceName)) {
                        Mixer mixer = AudioSystem.getMixer(mixerInfo);
                        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                        if (mixer.isLineSupported(info)) {
                            targetDataLine = (TargetDataLine) mixer.getLine(info);
                            targetDataLine.open(format, BUFFER_SIZE);
                            targetDataLine.start();

                            isStreamOpen = true;
                            lastError = null;
                            LOGGER.info("Audio stream opened from device: " + deviceName);
                            return true;
                        }
                    }
                } catch (LineUnavailableException e) {
                    LOGGER.warning("Line unavailable for mixer: " + mixerInfo.getName());
                    // Continue trying other mixers
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error opening mixer: " + mixerInfo.getName(), e);
                    // Continue trying other mixers
                }
            }

            // Device not found, fallback to default
            LOGGER.warning("Device not found: " + deviceName + ", falling back to default");
            return openStream();

        } catch (Exception e) {
            lastError = "Failed to open audio stream from device: " + e.getMessage();
            LOGGER.log(Level.SEVERE, lastError, e);

            // Fallback to default device
            LOGGER.info("Attempting fallback to default device");
            return openStream();
        }
    }

    /**
     * Read audio data from the stream with error handling
     * @return Array of audio samples as floats (-1.0 to 1.0), or empty array on error
     */
    public float[] readAudioData() {
        if (!isStreamOpen || targetDataLine == null) {
            LOGGER.fine("Cannot read audio: stream not open");
            return new float[0];
        }

        try {
            // Check if line is still active
            if (!targetDataLine.isActive()) {
                LOGGER.warning("Audio line is not active");
                return new float[0];
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

            if (bytesRead <= 0) {
                LOGGER.fine("No bytes read from audio stream");
                return new float[0];
            }

            // Convert bytes to floats
            float[] audioData = new float[bytesRead / 2];  // 2 bytes per sample (16-bit)

            for (int i = 0; i < audioData.length; i++) {
                try {
                    // Combine two bytes into one 16-bit sample (big endian)
                    int sample = ((buffer[i * 2] & 0xFF) << 8) | (buffer[i * 2 + 1] & 0xFF);

                    // Convert to signed
                    if (sample > 32767) {
                        sample -= 65536;
                    }

                    // Normalize to -1.0 to 1.0
                    audioData[i] = sample / 32768.0f;
                } catch (ArrayIndexOutOfBoundsException e) {
                    LOGGER.warning("Array index error during audio conversion at index " + i);
                    break; // Return partial data
                }
            }

            return audioData;

        } catch (Exception e) {
            lastError = "Error reading audio data: " + e.getMessage();
            LOGGER.log(Level.WARNING, lastError, e);
            return new float[0];
        }
    }

    /**
     * Close audio stream safely
     */
    public void closeStream() {
        try {
            if (targetDataLine != null) {
                if (targetDataLine.isActive()) {
                    targetDataLine.stop();
                }
                if (targetDataLine.isOpen()) {
                    targetDataLine.close();
                }
                isStreamOpen = false;
                LOGGER.info("Audio stream closed successfully");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing audio stream", e);
            isStreamOpen = false; // Mark as closed anyway
        } finally {
            // Clean up buffer pool
            if (bufferPool != null) {
                bufferPool.clear();
            }
        }
    }

    /**
     * Get available audio input devices with error handling
     */
    public static String[] getAvailableDevices() {
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers == null || mixers.length == 0) {
                LOGGER.warning("No audio mixers found");
                return new String[]{"No devices available"};
            }

            return java.util.Arrays.stream(mixers)
                .map(Mixer.Info::getName)
                .filter(name -> name != null && !name.isEmpty())
                .toArray(String[]::new);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting available devices", e);
            return new String[]{"Error getting devices"};
        }
    }

    // Getters
    public boolean isStreamOpen() {
        return isStreamOpen && targetDataLine != null && targetDataLine.isActive();
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

    public boolean hasAudioDevice() {
        return hasAudioDevice;
    }

    public String getLastError() {
        return lastError;
    }

    /**
     * Get buffer pool for advanced usage
     */
    public AudioBufferPool getBufferPool() {
        return bufferPool;
    }
}
