package org.echoiot.server.service.entitiy.tenant.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Service;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.echoiot.server.service.entitiy.queue.TbQueueService;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbTenantProfileService extends AbstractTbEntityService implements TbTenantProfileService {
    private final TbQueueService tbQueueService;
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public TenantProfile save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile) throws ThingsboardException {
        TenantProfile savedTenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(tenantId, tenantProfile));
        if (oldTenantProfile != null && savedTenantProfile.isIsolatedTbRuleEngine()) {
            List<TenantId> tenantIds = tenantService.findTenantIdsByTenantProfileId(savedTenantProfile.getId());
            tbQueueService.updateQueuesByTenants(tenantIds, savedTenantProfile, oldTenantProfile);
        }

        tenantProfileCache.put(savedTenantProfile);
        tbClusterService.onTenantProfileChange(savedTenantProfile, null);
        tbClusterService.broadcastEntityStateChangeEvent(TenantId.SYS_TENANT_ID, savedTenantProfile.getId(),
                tenantProfile.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

        return savedTenantProfile;
    }

    @Override
    public void delete(TenantId tenantId, TenantProfile tenantProfile) throws ThingsboardException {
        tenantProfileService.deleteTenantProfile(tenantId, tenantProfile.getId());
        tbClusterService.onTenantProfileDelete(tenantProfile, null);
    }
}
