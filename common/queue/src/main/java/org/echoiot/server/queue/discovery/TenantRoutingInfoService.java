package org.echoiot.server.queue.discovery;

import org.echoiot.server.common.data.id.TenantId;

public interface TenantRoutingInfoService {

    TenantRoutingInfo getRoutingInfo(TenantId tenantId);
}
