package org.echoiot.server.service.telemetry;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 27.03.18.
 */
@Data
public class TelemetryWebSocketTextMsg {

    @NotNull
    private final TelemetryWebSocketSessionRef sessionRef;
    @NotNull
    private final String payload;

}
