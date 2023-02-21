package org.echoiot.server.service.sync.ie.exporting.impl;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@TbCoreComponent
public class DeviceProfileExportService extends BaseEntityExportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    @Override
    protected void setRelatedEntities(@NotNull EntitiesExportCtx<?> ctx, @NotNull DeviceProfile deviceProfile, EntityExportData<DeviceProfile> exportData) {
        deviceProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, deviceProfile.getDefaultDashboardId()));
        deviceProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, deviceProfile.getDefaultRuleChainId()));
    }

    @NotNull
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DEVICE_PROFILE);
    }

}
