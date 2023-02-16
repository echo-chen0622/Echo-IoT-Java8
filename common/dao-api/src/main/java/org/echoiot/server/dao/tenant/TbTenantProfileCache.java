package org.echoiot.server.dao.tenant;

import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;

import java.util.function.Consumer;

public interface TbTenantProfileCache {

    TenantProfile get(TenantId tenantId);

    TenantProfile get(TenantProfileId tenantProfileId);

    void put(TenantProfile profile);

    void evict(TenantProfileId id);

    void evict(TenantId id);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<TenantProfile> profileListener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
