package org.echoiot.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

@Data
@Slf4j
public class DeviceIdInfo implements Serializable, HasTenantId {

    private static final long serialVersionUID = 2233745129677581815L;

    private final TenantId tenantId;
    @Nullable
    private final CustomerId customerId;
    private final DeviceId deviceId;

    public DeviceIdInfo(UUID tenantId, @Nullable UUID customerId, UUID deviceId) {
        this.tenantId = new TenantId(tenantId);
        this.customerId = customerId != null ? new CustomerId(customerId) : null;
        this.deviceId = new DeviceId(deviceId);
    }
}
