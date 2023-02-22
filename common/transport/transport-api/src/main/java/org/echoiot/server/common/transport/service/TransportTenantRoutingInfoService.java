package org.echoiot.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.transport.TransportTenantProfileCache;
import org.echoiot.server.queue.discovery.TenantRoutingInfo;
import org.echoiot.server.queue.discovery.TenantRoutingInfoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport'")
public class TransportTenantRoutingInfoService implements TenantRoutingInfoService {

    private final TransportTenantProfileCache tenantProfileCache;

    public TransportTenantRoutingInfoService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        TenantProfile profile = tenantProfileCache.get(tenantId);
        return new TenantRoutingInfo(tenantId, profile.isIsolatedTbRuleEngine());
    }

}
