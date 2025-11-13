package com.shredforge.scoring.detection;

public interface NoteDetectionEngine extends AutoCloseable {

    void start(NoteDetectionConfig config, NoteDetectionListener listener);

    void stop();

    boolean isRunning();

    @Override
    default void close() {
        stop();
    }
}
