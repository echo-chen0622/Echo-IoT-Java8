package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;

/**
 * @author Andrew Shvayka
 */
@Data
class SessionInfoMetaData {
    private final SessionInfo sessionInfo;
    private long lastActivityTime;
    private boolean subscribedToAttributes;
    private boolean subscribedToRPC;

    SessionInfoMetaData(SessionInfo sessionInfo) {
        this(sessionInfo, System.currentTimeMillis());
    }

    SessionInfoMetaData(SessionInfo sessionInfo, long lastActivityTime) {
        this.sessionInfo = sessionInfo;
        this.lastActivityTime = lastActivityTime;
    }
}
