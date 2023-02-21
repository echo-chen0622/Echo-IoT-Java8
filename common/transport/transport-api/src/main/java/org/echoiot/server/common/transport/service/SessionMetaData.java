package org.echoiot.server.common.transport.service;

import lombok.Data;
import org.echoiot.server.common.transport.SessionMsgListener;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by Echo on 15.10.18.
 */
@Data
public class SessionMetaData {

    private volatile TransportProtos.SessionInfoProto sessionInfo;
    private final TransportProtos.SessionType sessionType;
    private final SessionMsgListener listener;

    @Nullable
    private volatile ScheduledFuture scheduledFuture;
    private volatile boolean subscribedToAttributes;
    private volatile boolean subscribedToRPC;
    private volatile boolean overwriteActivityTime;

    SessionMetaData(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionType sessionType, SessionMsgListener listener) {
        this.sessionInfo = sessionInfo;
        this.sessionType = sessionType;
        this.listener = listener;
        this.scheduledFuture = null;
    }

    void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }

    public boolean hasScheduledFuture() {
        return null != this.scheduledFuture;
    }
}
