package org.echoiot.rule.engine.api;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by ashvayka on 02.04.18.
 */
@Data
@Builder
public final class RuleEngineDeviceRpcRequest {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    private final int requestId;
    @NotNull
    private final UUID requestUUID;
    @NotNull
    private final String originServiceId;
    private final boolean oneway;
    private final boolean persisted;
    @NotNull
    private final String method;
    @NotNull
    private final String body;
    private final long expirationTime;
    private final boolean restApiCall;
    @NotNull
    private final String additionalInfo;
    @NotNull
    private final Integer retries;
}
