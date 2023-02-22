package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos.SessionType;

/**
 * @author Andrew Shvayka
 */
@Data
public class SessionInfo {
    private final SessionType type;
    private final String nodeId;
}
