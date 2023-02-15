package org.thingsboard.server.service.telemetry;

/**
 * Created by ashvayka on 08.05.17.
 */
public enum TelemetryFeature {

    ATTRIBUTES, TIMESERIES;

    public static TelemetryFeature forName(String name) {
        return TelemetryFeature.valueOf(name.toUpperCase());
    }

}
