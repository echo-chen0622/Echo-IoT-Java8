package org.echoiot.server.dao.device;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
public class DeviceProfileEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;
    @NotNull
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;

}
