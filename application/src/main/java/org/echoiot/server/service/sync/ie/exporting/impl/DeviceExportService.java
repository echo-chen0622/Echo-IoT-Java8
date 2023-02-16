package org.echoiot.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.sync.ie.DeviceExportData;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.springframework.stereotype.Service;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceExportService extends BaseEntityExportService<DeviceId, Device, DeviceExportData> {

    private final DeviceCredentialsService deviceCredentialsService;

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Device device, DeviceExportData exportData) {
        device.setCustomerId(getExternalIdOrElseInternal(ctx, device.getCustomerId()));
        device.setDeviceProfileId(getExternalIdOrElseInternal(ctx, device.getDeviceProfileId()));
        if (ctx.getSettings().isExportCredentials()) {
            var credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), device.getId());
            credentials.setId(null);
            credentials.setDeviceId(null);
            exportData.setCredentials(credentials);
        }
    }

    @Override
    protected DeviceExportData newExportData() {
        return new DeviceExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DEVICE);
    }

}
