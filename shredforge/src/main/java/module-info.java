module com.shredforge {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires TarsosDSP.core;
    requires TarsosDSP.jvm;

    opens com.shredforge to javafx.fxml;
    opens com.shredforge.tabs.dao to com.fasterxml.jackson.databind;
    exports com.shredforge;
    exports com.shredforge.core;
    exports com.shredforge.core.model;
    exports com.shredforge.core.ports;
    exports com.shredforge.tabs;
    exports com.shredforge.tabs.model;
    exports com.shredforge.tabview;
    exports com.shredforge.tabview.render;
    exports com.shredforge.scoring;
    exports com.shredforge.scoring.model;
    exports com.shredforge.scoring.detection;
}
