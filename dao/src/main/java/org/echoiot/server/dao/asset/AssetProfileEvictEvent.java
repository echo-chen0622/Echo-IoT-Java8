package org.echoiot.server.dao.asset;

import lombok.Data;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;

@Data
public class AssetProfileEvictEvent {

    private final TenantId tenantId;
    private final String newName;
    private final String oldName;
    private final AssetProfileId assetProfileId;
    private final boolean defaultProfile;

}
