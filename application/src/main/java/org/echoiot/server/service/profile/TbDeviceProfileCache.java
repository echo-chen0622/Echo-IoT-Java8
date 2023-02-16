package org.echoiot.server.service.profile;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.rule.engine.api.RuleEngineDeviceProfileCache;

public interface TbDeviceProfileCache extends RuleEngineDeviceProfileCache {

    void evict(TenantId tenantId, DeviceProfileId id);

    void evict(TenantId tenantId, DeviceId id);

    DeviceProfile find(DeviceProfileId deviceProfileId);

    DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String deviceType);
}
