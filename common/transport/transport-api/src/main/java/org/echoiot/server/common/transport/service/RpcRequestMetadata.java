package org.echoiot.server.common.transport.service;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
public class RpcRequestMetadata {
    @NotNull
    private final UUID sessionId;
    private final int requestId;
}
