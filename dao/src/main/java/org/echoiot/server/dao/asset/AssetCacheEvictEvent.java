package org.echoiot.server.dao.asset;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor
class AssetCacheEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;

}
