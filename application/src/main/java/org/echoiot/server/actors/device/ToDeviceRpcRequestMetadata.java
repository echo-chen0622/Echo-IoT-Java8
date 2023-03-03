package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.service.rpc.ToDeviceRpcRequestActorMsg;

/**
 * @author Echo
 */
@Data
public class ToDeviceRpcRequestMetadata {
    private final ToDeviceRpcRequestActorMsg msg;
    private final boolean sent;
    private int retries;
    private boolean delivered;
}
