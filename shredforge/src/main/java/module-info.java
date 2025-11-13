module com.shredforge {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    // JSON processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    // Audio processing - TarsosDSP (automatic module)
    requires TarsosDSP;

    // Java standard modules
    requires java.logging;
    requires java.desktop;  // For javax.sound.sampled
    requires java.net.http;  // For HTTP client

    // Export packages
    exports com.shredforge;
    exports com.shredforge.model;
    exports com.shredforge.repository;
    exports com.shredforge.tab;
    exports com.shredforge.input;
    exports com.shredforge.notedetection;
    exports com.shredforge.calibration;
    exports com.shredforge.playback;
    exports com.shredforge.ui;
    exports com.shredforge.persistence;
    exports com.shredforge.demo;
    exports com.shredforge.util;

    // Open packages to JavaFX for FXML and reflection
    opens com.shredforge to javafx.fxml;
    opens com.shredforge.ui to javafx.fxml;
    opens com.shredforge.model to com.fasterxml.jackson.databind;
}
