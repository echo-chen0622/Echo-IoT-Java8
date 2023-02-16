package org.echoiot.server.service.subscription;

import org.echoiot.server.service.telemetry.cmd.v2.AlarmDataCmd;
import org.echoiot.server.service.telemetry.cmd.v2.EntityCountCmd;
import org.echoiot.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.echoiot.server.service.telemetry.cmd.v2.UnsubscribeCmd;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;

public interface TbEntityDataSubscriptionService {

    void handleCmd(TelemetryWebSocketSessionRef sessionId, EntityDataCmd cmd);

    void handleCmd(TelemetryWebSocketSessionRef sessionId, EntityCountCmd cmd);

    void handleCmd(TelemetryWebSocketSessionRef sessionId, AlarmDataCmd cmd);

    void cancelSubscription(String sessionId, UnsubscribeCmd subscriptionId);

    void cancelAllSessionSubscriptions(String sessionId);

}
