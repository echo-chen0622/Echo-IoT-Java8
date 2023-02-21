package org.echoiot.server.cache.device;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class DeviceCacheKey implements Serializable {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    @NotNull
    private final String deviceName;

    public DeviceCacheKey(TenantId tenantId, DeviceId deviceId) {
        this(tenantId, deviceId, null);
    }

    public DeviceCacheKey(TenantId tenantId, String deviceName) {
        this(tenantId, null, deviceName);
    }

    @NotNull
    @Override
    public String toString() {
        if (deviceId != null) {
            return tenantId + "_" + deviceId;
        } else {
            return tenantId + "_n_" + deviceName;
        }
    }

}
