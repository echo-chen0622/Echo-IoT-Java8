package org.echoiot.server.service.telemetry;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 08.05.17.
 */
public enum TelemetryFeature {

    ATTRIBUTES, TIMESERIES;

    @NotNull
    public static TelemetryFeature forName(@NotNull String name) {
        return TelemetryFeature.valueOf(name.toUpperCase());
    }

}
