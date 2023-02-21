package org.echoiot.server.common.msg.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequest implements Serializable {
    @NotNull
    private final UUID id;
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    private final boolean oneway;
    private final long expirationTime;
    @NotNull
    private final ToDeviceRpcRequestBody body;
    private final boolean persisted;
    @NotNull
    private final Integer retries;
    @NotNull
    @JsonIgnore
    private final String additionalInfo;
}
