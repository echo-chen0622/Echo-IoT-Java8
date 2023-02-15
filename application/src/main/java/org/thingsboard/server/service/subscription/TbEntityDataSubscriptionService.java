package org.thingsboard.server.service.subscription;

import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUnsubscribeCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.UnsubscribeCmd;

public interface TbEntityDataSubscriptionService {

    void handleCmd(TelemetryWebSocketSessionRef sessionId, EntityDataCmd cmd);

    void handleCmd(TelemetryWebSocketSessionRef sessionId, EntityCountCmd cmd);

    void handleCmd(TelemetryWebSocketSessionRef sessionId, AlarmDataCmd cmd);

    void cancelSubscription(String sessionId, UnsubscribeCmd subscriptionId);

    void cancelAllSessionSubscriptions(String sessionId);

}
