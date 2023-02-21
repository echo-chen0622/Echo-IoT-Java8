package org.echoiot.server.dao.tenant;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
public class TenantEvictEvent {
    @NotNull
    private final TenantId tenantId;
    private final boolean invalidateExists;
}
