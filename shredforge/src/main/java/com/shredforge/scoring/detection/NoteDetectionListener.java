package com.shredforge.scoring.detection;

import com.shredforge.scoring.model.DetectedNote;

public interface NoteDetectionListener {

    void onNoteDetected(DetectedNote note);

    default void onError(Throwable throwable) {
        throwable.printStackTrace();
    }
}
