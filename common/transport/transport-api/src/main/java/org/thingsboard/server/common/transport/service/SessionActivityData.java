package org.thingsboard.server.common.transport.service;

import lombok.Data;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by ashvayka on 15.10.18.
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
