package org.echoiot.server.dao.device;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public static DeviceProfileCacheKey fromName(TenantId tenantId, String name) {
        return new DeviceProfileCacheKey(tenantId, name, null, false);
    }

    @NotNull
    public static DeviceProfileCacheKey fromId(DeviceProfileId id) {
        return new DeviceProfileCacheKey(null, null, id, false);
    }

    @NotNull
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
