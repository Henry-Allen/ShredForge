package com.shredforge;

import com.shredforge.calibration.TuningLibrary;
import com.shredforge.calibration.TuningPreset;
import com.shredforge.core.model.CalibrationInput;
import com.shredforge.core.model.CalibrationProfile;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public final class CalibrationController {

    @FXML
    private ListView<TuningPreset> tuningList;

    @FXML
    private Label tuningDetailsLabel;

    @FXML
    private Label calibrationStatusLabel;

    @FXML
    private Button startCalibrationButton;

    private final DecimalFormat centsFormat = new DecimalFormat("+0.0;-0.0");

    @FXML
    public void initialize() {
        tuningList.setItems(FXCollections.observableArrayList(TuningLibrary.commonTunings()));
        tuningList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TuningPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        tuningList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            updatePresetDetails(newSel);
            calibrationStatusLabel.setText("Select \"Calibrate\" to analyze this tuning.");
        });
        if (!tuningList.getItems().isEmpty()) {
            tuningList.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onStartCalibration() {
        TuningPreset preset = tuningList.getSelectionModel().getSelectedItem();
        if (preset == null) {
            calibrationStatusLabel.setText("Pick a tuning preset first.");
            return;
        }
        CalibrationInput input = new CalibrationInput(
                "demo-user",
                32.0,
                preset.stringFrequenciesHz());
        CalibrationProfile profile = App.repository().calibrate(input);
        calibrationStatusLabel.setText(renderProfile(profile));
    }

    @FXML
    private void onBackToTabs() {
        App.showPrimary();
    }

    private void updatePresetDetails(TuningPreset preset) {
        if (preset == null) {
            tuningDetailsLabel.setText("Select a tuning to see target frequencies.");
            startCalibrationButton.setDisable(true);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(preset.name()).append(System.lineSeparator());
        preset.stringFrequenciesHz().forEach((string, freq) -> sb
                .append(" • ")
                .append(string.toUpperCase(Locale.ROOT))
                .append(" → ")
                .append(String.format(Locale.US, "%.2f Hz", freq))
                .append(System.lineSeparator()));
        tuningDetailsLabel.setText(sb.toString());
        startCalibrationButton.setDisable(false);
    }

    private String renderProfile(CalibrationProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gain: ").append(String.format(Locale.US, "%.1f dB", profile.inputGain())).append(" | Noise floor: ")
                .append(String.format(Locale.US, "%.1f dB", profile.noiseFloorDb()))
                .append(System.lineSeparator())
                .append("String offsets (cents):")
                .append(System.lineSeparator());
        for (Map.Entry<String, Double> entry : profile.stringOffsetsCents().entrySet()) {
            sb.append("   ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(centsFormat.format(entry.getValue()))
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }
}
