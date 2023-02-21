package org.echoiot.server.dao.tenant;

import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface TbTenantProfileCache {

    @Nullable
    TenantProfile get(TenantId tenantId);

    @Nullable
    TenantProfile get(TenantProfileId tenantProfileId);

    void put(TenantProfile profile);

    void evict(TenantProfileId id);

    void evict(TenantId id);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<TenantProfile> profileListener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
