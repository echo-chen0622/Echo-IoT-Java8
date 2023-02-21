package org.echoiot.server.dao.edge;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor
class EdgeCacheEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;

}
