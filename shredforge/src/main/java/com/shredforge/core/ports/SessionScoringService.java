package com.shredforge.core.ports;

import com.shredforge.core.model.SessionRequest;
import com.shredforge.core.model.SessionResult;

/**
 * Compares live guitar input to the active tab and produces a score.
 */
public interface SessionScoringService {

    SessionResult score(SessionRequest request);
}
