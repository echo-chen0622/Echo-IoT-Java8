package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;

/**
 * @author Andrew Shvayka
 */
@Data
public class SessionInfo {
    private final SessionType type;
    private final String nodeId;
}
