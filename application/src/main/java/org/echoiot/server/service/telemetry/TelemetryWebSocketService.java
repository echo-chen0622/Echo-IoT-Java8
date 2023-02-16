package org.echoiot.server.service.telemetry;

import org.echoiot.server.service.telemetry.cmd.v2.CmdUpdate;
import org.echoiot.server.service.telemetry.sub.TelemetrySubscriptionUpdate;
import org.springframework.web.socket.CloseStatus;

/**
 * Created by Echo on 27.03.18.
 */
public interface TelemetryWebSocketService {

    void handleWebSocketSessionEvent(TelemetryWebSocketSessionRef sessionRef, SessionEvent sessionEvent);

    void handleWebSocketMsg(TelemetryWebSocketSessionRef sessionRef, String msg);

    void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate update);

    void sendWsMsg(String sessionId, CmdUpdate update);

    void close(String sessionId, CloseStatus status);
}
