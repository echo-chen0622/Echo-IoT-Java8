package org.echoiot.server.common.msg.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rpc.ToDeviceRpcRequestBody;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class ToDeviceRpcRequest implements Serializable {
    private final UUID id;
    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final boolean oneway;
    private final long expirationTime;
    private final ToDeviceRpcRequestBody body;
    private final boolean persisted;
    private final Integer retries;
    @JsonIgnore
    private final String additionalInfo;
}
