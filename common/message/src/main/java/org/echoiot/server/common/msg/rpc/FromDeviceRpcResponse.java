package org.echoiot.server.common.msg.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.rpc.RpcError;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@RequiredArgsConstructor
@ToString
public class FromDeviceRpcResponse implements Serializable {
    @NotNull
    @Getter
    private final UUID id;
    @NotNull
    private final String response;
    @NotNull
    private final RpcError error;

    @NotNull
    public Optional<String> getResponse() {
        return Optional.ofNullable(response);
    }

    @NotNull
    public Optional<RpcError> getError() {
        return Optional.ofNullable(error);
    }

}
