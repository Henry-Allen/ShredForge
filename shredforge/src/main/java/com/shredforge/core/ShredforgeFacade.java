package com.shredforge.core;

import com.shredforge.core.model.CalibrationInput;
import com.shredforge.core.model.CalibrationProfile;
import com.shredforge.core.model.FormattedTab;
import com.shredforge.core.model.SessionRequest;
import com.shredforge.core.model.SessionResult;
import com.shredforge.core.model.SongRequest;
import com.shredforge.core.model.TabData;
import com.shredforge.core.ports.CalibrationService;
import com.shredforge.core.ports.SessionScoringService;
import com.shredforge.core.ports.TabFormatter;
import com.shredforge.core.ports.TabGateway;
import java.time.Instant;
import java.util.Objects;

/**
 * Facade that coordinates all Shredforge subsystems. Users should interact with {@code ShredforgeRepository} which
 * delegates to this class.
 */
public final class ShredforgeFacade {

    private final TabGateway tabGateway;
    private final TabFormatter tabFormatter;
    private final CalibrationService calibrationService;
    private final SessionScoringService sessionScoringService;

    private ShredforgeFacade(Builder builder) {
        this.tabGateway = builder.tabGateway;
        this.tabFormatter = builder.tabFormatter;
        this.calibrationService = builder.calibrationService;
        this.sessionScoringService = builder.sessionScoringService;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TabData downloadTab(SongRequest request) {
        Objects.requireNonNull(request, "request");
        return ensure(tabGateway, "TabGateway").fetchTab(request);
    }

    public void saveTab(TabData tabData) {
        Objects.requireNonNull(tabData, "tabData");
        ensure(tabGateway, "TabGateway").persistTab(tabData);
    }

    public FormattedTab formatTab(TabData tabData) {
        Objects.requireNonNull(tabData, "tabData");
        return ensure(tabFormatter, "TabFormatter").format(tabData);
    }

    public FormattedTab downloadAndFormatTab(SongRequest request) {
        TabData tabData = downloadTab(request);
        return formatTab(tabData);
    }

    public CalibrationProfile calibrate(CalibrationInput input) {
        Objects.requireNonNull(input, "input");
        return ensure(calibrationService, "CalibrationService").calibrate(input);
    }

    public SessionResult runSession(SessionRequest request) {
        Objects.requireNonNull(request, "request");
        return ensure(sessionScoringService, "SessionScoringService").score(request);
    }

    public SessionResult runSession(String userId, TabData tabData, CalibrationProfile calibrationProfile) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tabData, "tabData");
        Objects.requireNonNull(calibrationProfile, "calibrationProfile");
        SessionRequest request = new SessionRequest(userId, tabData, calibrationProfile, Instant.now());
        return runSession(request);
    }

    public RepositoryState describeState() {
        return new RepositoryState(
                tabGateway != null, tabFormatter != null, calibrationService != null, sessionScoringService != null);
    }

    private static <T> T ensure(T dependency, String friendlyName) {
        if (dependency == null) {
            throw new IllegalStateException(friendlyName + " has not been configured on ShredforgeFacade yet.");
        }
        return dependency;
    }

    public static final class Builder {
        private TabGateway tabGateway;
        private TabFormatter tabFormatter;
        private CalibrationService calibrationService;
        private SessionScoringService sessionScoringService;

        public Builder withTabGateway(TabGateway tabGateway) {
            this.tabGateway = Objects.requireNonNull(tabGateway, "tabGateway");
            return this;
        }

        public Builder withTabFormatter(TabFormatter tabFormatter) {
            this.tabFormatter = Objects.requireNonNull(tabFormatter, "tabFormatter");
            return this;
        }

        public Builder withCalibrationService(CalibrationService calibrationService) {
            this.calibrationService = Objects.requireNonNull(calibrationService, "calibrationService");
            return this;
        }

        public Builder withSessionScoringService(SessionScoringService sessionScoringService) {
            this.sessionScoringService =
                    Objects.requireNonNull(sessionScoringService, "sessionScoringService");
            return this;
        }

        public ShredforgeFacade build() {
            return new ShredforgeFacade(this);
        }
    }

    public record RepositoryState(
            boolean tabGatewayConfigured,
            boolean tabFormatterConfigured,
            boolean calibrationServiceConfigured,
            boolean sessionScoringServiceConfigured) {

        public boolean isReadyForSessions() {
            return tabGatewayConfigured
                    && tabFormatterConfigured
                    && calibrationServiceConfigured
                    && sessionScoringServiceConfigured;
        }
    }
}
