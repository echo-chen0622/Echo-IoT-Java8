package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineDeviceProfileCache {

    DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile get(TenantId tenantId, DeviceId deviceId);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> devicelistener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
