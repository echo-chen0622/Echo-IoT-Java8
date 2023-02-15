package org.thingsboard.server.dao.asset;

import lombok.Data;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class AssetProfileCacheKey implements Serializable {

    private static final long serialVersionUID = 8220455917177676472L;

    private final TenantId tenantId;
    private final String name;
    private final AssetProfileId assetProfileId;
    private final boolean defaultProfile;

    private AssetProfileCacheKey(TenantId tenantId, String name, AssetProfileId assetProfileId, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.name = name;
        this.assetProfileId = assetProfileId;
        this.defaultProfile = defaultProfile;
    }

    public static AssetProfileCacheKey fromName(TenantId tenantId, String name) {
        return new AssetProfileCacheKey(tenantId, name, null, false);
    }

    public static AssetProfileCacheKey fromId(AssetProfileId id) {
        return new AssetProfileCacheKey(null, null, id, false);
    }

    public static AssetProfileCacheKey defaultProfile(TenantId tenantId) {
        return new AssetProfileCacheKey(tenantId, null, null, true);
    }

    @Override
    public String toString() {
        if (assetProfileId != null) {
            return assetProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else {
            return tenantId + "_" + name;
        }
    }
}
