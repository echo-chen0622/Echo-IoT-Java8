package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.ota.OtaPackageStateService;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceProfileImportService extends BaseEntityImportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    @NotNull
    private final DeviceProfileService deviceProfileService;
    @NotNull
    private final OtaPackageStateService otaPackageStateService;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull DeviceProfile deviceProfile, IdProvider idProvider) {
        deviceProfile.setTenantId(tenantId);
    }

    @NotNull
    @Override
    protected DeviceProfile prepare(EntitiesImportCtx ctx, @NotNull DeviceProfile deviceProfile, DeviceProfile old, EntityExportData<DeviceProfile> exportData, @NotNull IdProvider idProvider) {
        deviceProfile.setDefaultRuleChainId(idProvider.getInternalId(deviceProfile.getDefaultRuleChainId()));
        deviceProfile.setDefaultDashboardId(idProvider.getInternalId(deviceProfile.getDefaultDashboardId()));
        deviceProfile.setFirmwareId(getOldEntityField(old, DeviceProfile::getFirmwareId));
        deviceProfile.setSoftwareId(getOldEntityField(old, DeviceProfile::getSoftwareId));
        return deviceProfile;
    }

    @Override
    protected DeviceProfile saveOrUpdate(EntitiesImportCtx ctx, DeviceProfile deviceProfile, EntityExportData<DeviceProfile> exportData, IdProvider idProvider) {
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Override
    protected void onEntitySaved(@NotNull User user, @NotNull DeviceProfile savedDeviceProfile, @Nullable DeviceProfile oldDeviceProfile) throws EchoiotException {
        clusterService.onDeviceProfileChange(savedDeviceProfile, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedDeviceProfile.getId(),
                oldDeviceProfile == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        otaPackageStateService.update(savedDeviceProfile,
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getFirmwareId(), savedDeviceProfile.getFirmwareId()),
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getSoftwareId(), savedDeviceProfile.getSoftwareId()));
        entityNotificationService.notifyCreateOrUpdateOrDelete(savedDeviceProfile.getTenantId(), null,
                                                               savedDeviceProfile.getId(), savedDeviceProfile, user, oldDeviceProfile == null ? ActionType.ADDED : ActionType.UPDATED, true, null);
    }

    @NotNull
    @Override
    protected DeviceProfile deepCopy(@NotNull DeviceProfile deviceProfile) {
        return new DeviceProfile(deviceProfile);
    }

    @Override
    protected void cleanupForComparison(@NotNull DeviceProfile deviceProfile) {
        super.cleanupForComparison(deviceProfile);
        deviceProfile.setFirmwareId(null);
        deviceProfile.setSoftwareId(null);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
