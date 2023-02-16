package org.echoiot.server.service.telemetry;

/**
 * Created by Echo on 08.05.17.
 */
public enum TelemetryFeature {

    ATTRIBUTES, TIMESERIES;

    public static TelemetryFeature forName(String name) {
        return TelemetryFeature.valueOf(name.toUpperCase());
    }

}
