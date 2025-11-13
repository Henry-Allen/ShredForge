package com.shredforge.core.ports;

import com.shredforge.core.model.CalibrationInput;
import com.shredforge.core.model.CalibrationProfile;

/**
 * Builds calibration profiles for a user + instrument pairing.
 */
public interface CalibrationService {

    CalibrationProfile calibrate(CalibrationInput input);
}
