package org.thingsboard.server.common.msg.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.rpc.RpcError;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@RequiredArgsConstructor
@ToString
public class FromDeviceRpcResponse implements Serializable {
    @Getter
    private final UUID id;
    private final String response;
    private final RpcError error;

    public Optional<String> getResponse() {
        return Optional.ofNullable(response);
    }

    public Optional<RpcError> getError() {
        return Optional.ofNullable(error);
    }

}
