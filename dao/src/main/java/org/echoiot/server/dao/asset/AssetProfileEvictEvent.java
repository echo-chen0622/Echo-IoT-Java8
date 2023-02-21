package org.echoiot.server.dao.asset;

import lombok.Data;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
public class AssetProfileEvictEvent {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final String newName;
    @NotNull
    private final String oldName;
    @NotNull
    private final AssetProfileId assetProfileId;
    private final boolean defaultProfile;

}
