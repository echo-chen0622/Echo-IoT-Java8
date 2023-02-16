package org.echoiot.server.service.telemetry;

import lombok.Data;

/**
 * Created by Echo on 27.03.18.
 */
@Data
public class TelemetryWebSocketTextMsg {

    private final TelemetryWebSocketSessionRef sessionRef;
    private final String payload;

}
