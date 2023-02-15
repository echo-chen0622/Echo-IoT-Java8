package org.thingsboard.server.dao.asset;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@RequiredArgsConstructor
class AssetCacheEvictEvent {

    private final TenantId tenantId;
    private final String newName;
    private final String oldName;

}
