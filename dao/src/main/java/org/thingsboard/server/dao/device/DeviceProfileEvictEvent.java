package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class DeviceProfileEvictEvent {

    private final TenantId tenantId;
    private final String newName;
    private final String oldName;
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;

}
