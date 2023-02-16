package org.echoiot.server.common.transport;

import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.id.TenantId;

import java.util.Optional;

public interface TransportResourceCache {

    Optional<TbResource> get(TenantId tenantId, ResourceType resourceType, String resourceId);

    void update(TenantId tenantId, ResourceType resourceType, String resourceI);

    void evict(TenantId tenantId, ResourceType resourceType, String resourceId);
}
