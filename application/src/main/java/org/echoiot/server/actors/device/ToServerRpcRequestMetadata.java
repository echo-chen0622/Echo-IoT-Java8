package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToServerRpcRequestMetadata {
    @NotNull
    private final UUID sessionId;
    @NotNull
    private final TransportProtos.SessionType type;
    @NotNull
    private final String nodeId;
}
