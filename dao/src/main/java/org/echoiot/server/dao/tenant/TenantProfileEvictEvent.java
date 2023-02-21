package org.echoiot.server.dao.tenant;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.jetbrains.annotations.NotNull;

@Data
public class TenantProfileEvictEvent {
    @NotNull
    private final TenantProfileId tenantProfileId;
    private final boolean defaultProfile;
}
