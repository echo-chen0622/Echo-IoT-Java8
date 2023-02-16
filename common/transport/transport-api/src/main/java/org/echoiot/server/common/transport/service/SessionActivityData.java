package org.echoiot.server.common.transport.service;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos;

/**
 * Created by Echo on 15.10.18.
 */
@Data
public class SessionActivityData {

    private volatile TransportProtos.SessionInfoProto sessionInfo;
    private volatile long lastActivityTime;
    private volatile long lastReportedActivityTime;

    SessionActivityData(TransportProtos.SessionInfoProto sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }

}
