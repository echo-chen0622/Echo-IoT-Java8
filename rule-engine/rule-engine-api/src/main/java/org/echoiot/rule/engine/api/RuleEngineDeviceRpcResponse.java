package org.echoiot.rule.engine.api;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.rpc.RpcError;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Created by ashvayka on 02.04.18.
 */
@Data
@Builder
public final class RuleEngineDeviceRpcResponse {

    @NotNull
    private final DeviceId deviceId;
    private final int requestId;
    @NotNull
    private final Optional<String> response;
    @NotNull
    private final Optional<RpcError> error;

}
