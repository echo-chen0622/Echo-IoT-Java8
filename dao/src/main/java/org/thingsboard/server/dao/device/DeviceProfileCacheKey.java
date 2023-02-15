package org.thingsboard.server.dao.device;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class DeviceProfileCacheKey implements Serializable {

    private static final long serialVersionUID = 8220455917177676472L;

    private final TenantId tenantId;
    private final String name;
    private final DeviceProfileId deviceProfileId;
    private final boolean defaultProfile;

    private DeviceProfileCacheKey(TenantId tenantId, String name, DeviceProfileId deviceProfileId, boolean defaultProfile) {
        this.tenantId = tenantId;
        this.name = name;
        this.deviceProfileId = deviceProfileId;
        this.defaultProfile = defaultProfile;
    }

    public static DeviceProfileCacheKey fromName(TenantId tenantId, String name) {
        return new DeviceProfileCacheKey(tenantId, name, null, false);
    }

    public static DeviceProfileCacheKey fromId(DeviceProfileId id) {
        return new DeviceProfileCacheKey(null, null, id, false);
    }

    public static DeviceProfileCacheKey defaultProfile(TenantId tenantId) {
        return new DeviceProfileCacheKey(tenantId, null, null, true);
    }

    @Override
    public String toString() {
        if (deviceProfileId != null) {
            return deviceProfileId.toString();
        } else if (defaultProfile) {
            return tenantId.toString();
        } else {
            return tenantId + "_" + name;
        }
    }
}
