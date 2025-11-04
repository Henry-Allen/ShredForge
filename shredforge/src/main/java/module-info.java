module com.shredforge {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.shredforge to javafx.fxml;
    exports com.shredforge;
}
