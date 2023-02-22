package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos;

import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToServerRpcRequestMetadata {
    private final UUID sessionId;
    private final TransportProtos.SessionType type;
    private final String nodeId;
}
