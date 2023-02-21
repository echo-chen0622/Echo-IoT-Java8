package org.echoiot.server.dao.tenant;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
public class TenantProfileCacheKey implements Serializable {

    private static final long serialVersionUID = 8220455917177676472L;

    private final TenantProfileId tenantProfileId;
    private final boolean defaultProfile;

    private TenantProfileCacheKey(TenantProfileId tenantProfileId, boolean defaultProfile) {
        this.tenantProfileId = tenantProfileId;
        this.defaultProfile = defaultProfile;
    }

    @NotNull
    public static TenantProfileCacheKey fromId(TenantProfileId id) {
        return new TenantProfileCacheKey(id, false);
    }

    @NotNull
    public static TenantProfileCacheKey defaultProfile() {
        return new TenantProfileCacheKey(null, true);
    }


    @Override
    public String toString() {
        if (defaultProfile) {
            return "default";
        } else {
            return tenantProfileId.toString();
        }
    }
}
