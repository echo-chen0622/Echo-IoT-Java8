package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos.SessionType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
@Data
public class SessionInfo {
    @NotNull
    private final SessionType type;
    @NotNull
    private final String nodeId;
}
