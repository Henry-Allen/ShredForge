module com.shredforge {
    requires java.net.http;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires TarsosDSP.core;
    requires TarsosDSP.jvm;
    requires jcefmaven;
    requires jcef;
    requires com.formdev.flatlaf;

    opens com.shredforge.tabs.dao to com.fasterxml.jackson.databind;
    exports com.shredforge;
    exports com.shredforge.core;
    exports com.shredforge.core.model;
    exports com.shredforge.tabs;
    exports com.shredforge.tabs.model;
    exports com.shredforge.scoring;
    exports com.shredforge.calibration;
    exports com.shredforge.scoring.model;
    exports com.shredforge.scoring.detection;
    exports com.shredforge.ui;
}
