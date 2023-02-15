package org.thingsboard.server.actors.device;

import lombok.Data;
import org.thingsboard.server.service.rpc.ToDeviceRpcRequestActorMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequestMetadata {
    private final ToDeviceRpcRequestActorMsg msg;
    private final boolean sent;
    private int retries;
    private boolean delivered;
}
