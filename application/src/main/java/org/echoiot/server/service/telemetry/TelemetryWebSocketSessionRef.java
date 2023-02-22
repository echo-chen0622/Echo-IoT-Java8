package org.echoiot.server.service.telemetry;

import lombok.Getter;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Echo on 27.03.18.
 */
public class TelemetryWebSocketSessionRef {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String sessionId;
    @Getter
    private final SecurityUser securityCtx;
    @Getter
    private final InetSocketAddress localAddress;
    @Getter
    private final InetSocketAddress remoteAddress;
    @Getter
    private final AtomicInteger sessionSubIdSeq;

    public TelemetryWebSocketSessionRef(String sessionId, SecurityUser securityCtx, InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.sessionSubIdSeq = new AtomicInteger();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelemetryWebSocketSessionRef that = (TelemetryWebSocketSessionRef) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "TelemetryWebSocketSessionRef{" +
                "sessionId='" + sessionId + '\'' +
                ", localAddress=" + localAddress +
                ", remoteAddress=" + remoteAddress +
                '}';
    }
}
