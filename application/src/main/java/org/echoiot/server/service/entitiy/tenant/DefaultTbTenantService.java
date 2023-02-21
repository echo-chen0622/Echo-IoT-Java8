package org.echoiot.server.service.entitiy.tenant;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.echoiot.server.service.entitiy.queue.TbQueueService;
import org.echoiot.server.service.install.InstallScripts;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbTenantService extends AbstractTbEntityService implements TbTenantService {

    @NotNull
    private final TenantService tenantService;
    @NotNull
    private final TbTenantProfileCache tenantProfileCache;
    @NotNull
    private final InstallScripts installScripts;
    @NotNull
    private final TbQueueService tbQueueService;
    @NotNull
    private final TenantProfileService tenantProfileService;
    @NotNull
    private final EntitiesVersionControlService versionControlService;

    @NotNull
    @Override
    public Tenant save(@NotNull Tenant tenant) throws Exception {
        boolean created = tenant.getId() == null;
        Tenant oldTenant = !created ? tenantService.findTenantById(tenant.getId()) : null;

        Tenant savedTenant = checkNotNull(tenantService.saveTenant(tenant));
        if (created) {
            installScripts.createDefaultRuleChains(savedTenant.getId());
            installScripts.createDefaultEdgeRuleChains(savedTenant.getId());
        }
        tenantProfileCache.evict(savedTenant.getId());
        notificationEntityService.notifyCreateOrUpdateTenant(savedTenant, created ?
                ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

        TenantProfile oldTenantProfile = oldTenant != null ? tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, oldTenant.getTenantProfileId()) : null;
        TenantProfile newTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenant.getTenantProfileId());
        tbQueueService.updateQueuesByTenants(Collections.singletonList(savedTenant.getTenantId()), newTenantProfile, oldTenantProfile);
        return savedTenant;
    }

    @Override
    public void delete(@NotNull Tenant tenant) throws Exception {
        TenantId tenantId = tenant.getId();
        tenantService.deleteTenant(tenantId);
        tenantProfileCache.evict(tenantId);
        notificationEntityService.notifyDeleteTenant(tenant);
        versionControlService.deleteVersionControlSettings(tenantId).get(1, TimeUnit.MINUTES);
    }
}
