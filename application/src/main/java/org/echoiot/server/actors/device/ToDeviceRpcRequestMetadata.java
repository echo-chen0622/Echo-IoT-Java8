package org.echoiot.server.actors.device;

import lombok.Data;
import org.echoiot.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequestMetadata {
    @NotNull
    private final ToDeviceRpcRequestActorMsg msg;
    private final boolean sent;
    private int retries;
    private boolean delivered;
}
