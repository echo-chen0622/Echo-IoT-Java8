package org.echoiot.server.cache.device;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
public class DeviceCacheEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;

}
