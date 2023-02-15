package org.thingsboard.server.cache.device;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class DeviceCacheEvictEvent {

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final String newName;
    private final String oldName;

}
