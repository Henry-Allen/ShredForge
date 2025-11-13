module com.shredforge {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;
    requires com.fasterxml.jackson.databind;

    // TarsosDSP is not a JPMS module, so it's automatically available

    opens com.shredforge to javafx.fxml;
    exports com.shredforge;
}
