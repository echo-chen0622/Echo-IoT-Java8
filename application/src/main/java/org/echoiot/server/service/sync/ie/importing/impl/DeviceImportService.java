package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.ie.DeviceExportData;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceImportService extends BaseEntityImportService<DeviceId, Device, DeviceExportData> {

    @NotNull
    private final DeviceService deviceService;
    @NotNull
    private final DeviceCredentialsService credentialsService;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull Device device, @NotNull IdProvider idProvider) {
        device.setTenantId(tenantId);
        device.setCustomerId(idProvider.getInternalId(device.getCustomerId()));
    }

    @NotNull
    @Override
    protected Device prepare(EntitiesImportCtx ctx, @NotNull Device device, Device old, DeviceExportData exportData, @NotNull IdProvider idProvider) {
        device.setDeviceProfileId(idProvider.getInternalId(device.getDeviceProfileId()));
        device.setFirmwareId(getOldEntityField(old, Device::getFirmwareId));
        device.setSoftwareId(getOldEntityField(old, Device::getSoftwareId));
        return device;
    }

    @NotNull
    @Override
    protected Device deepCopy(@NotNull Device d) {
        return new Device(d);
    }

    @Override
    protected void cleanupForComparison(@NotNull Device e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    protected Device saveOrUpdate(@NotNull EntitiesImportCtx ctx, Device device, @NotNull DeviceExportData exportData, IdProvider idProvider) {
        if (exportData.getCredentials() != null && ctx.isSaveCredentials()) {
            exportData.getCredentials().setId(null);
            exportData.getCredentials().setDeviceId(null);
            return deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());
        } else {
            return deviceService.saveDevice(device);
        }
    }

    @Override
    protected boolean updateRelatedEntitiesIfUnmodified(@NotNull EntitiesImportCtx ctx, @NotNull Device prepared, @NotNull DeviceExportData exportData, IdProvider idProvider) {
        boolean updated = super.updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider);
        var credentials = exportData.getCredentials();
        if (credentials != null && ctx.isSaveCredentials()) {
            var existing = credentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), prepared.getId());
            credentials.setId(existing.getId());
            credentials.setDeviceId(prepared.getId());
            if (!existing.equals(credentials)) {
                credentialsService.updateDeviceCredentials(ctx.getTenantId(), credentials);
                updated = true;
            }
        }
        return updated;
    }

    @Override
    protected void onEntitySaved(@NotNull User user, @NotNull Device savedDevice, @Nullable Device oldDevice) throws EchoiotException {
        entityNotificationService.notifyCreateOrUpdateDevice(user.getTenantId(), savedDevice.getId(), savedDevice.getCustomerId(),
                                                             savedDevice, oldDevice, oldDevice == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
