package org.thingsboard.server.common.transport.service;

import lombok.Data;

import java.util.UUID;

@Data
public class RpcRequestMetadata {
    private final UUID sessionId;
    private final int requestId;
}
