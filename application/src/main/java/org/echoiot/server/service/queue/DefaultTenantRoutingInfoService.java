package org.echoiot.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.discovery.TenantRoutingInfo;
import org.echoiot.server.queue.discovery.TenantRoutingInfoService;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultTenantRoutingInfoService implements TenantRoutingInfoService {

    private final TenantService tenantService;

    private final TbTenantProfileCache tenantProfileCache;

    public DefaultTenantRoutingInfoService(TenantService tenantService, TbTenantProfileCache tenantProfileCache) {
        this.tenantService = tenantService;
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        @Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile != null) {
            return new TenantRoutingInfo(tenantId, tenantProfile.isIsolatedTbRuleEngine());
        } else {
            throw new RuntimeException("Tenant not found!");
        }
    }
}
