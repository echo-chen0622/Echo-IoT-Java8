package org.thingsboard.server.service.telemetry;

import lombok.Data;

/**
 * Created by ashvayka on 27.03.18.
 */
@Data
public class TelemetryWebSocketTextMsg {

    private final TelemetryWebSocketSessionRef sessionRef;
    private final String payload;

}
