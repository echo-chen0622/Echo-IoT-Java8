package org.thingsboard.server.service.telemetry;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface TelemetryWebSocketMsgEndpoint {

    void send(TelemetryWebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException;

    void sendPing(TelemetryWebSocketSessionRef sessionRef, long currentTime) throws IOException;

    void close(TelemetryWebSocketSessionRef sessionRef, CloseStatus withReason) throws IOException;
}
