package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Objects;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DeviceProfileImportService extends BaseEntityImportService<DeviceProfileId, DeviceProfile, EntityExportData<DeviceProfile>> {

    private final DeviceProfileService deviceProfileService;
    private final OtaPackageStateService otaPackageStateService;

    @Override
    protected void setOwner(TenantId tenantId, DeviceProfile deviceProfile, IdProvider idProvider) {
        deviceProfile.setTenantId(tenantId);
    }

    @Override
    protected DeviceProfile prepare(EntitiesImportCtx ctx, DeviceProfile deviceProfile, DeviceProfile old, EntityExportData<DeviceProfile> exportData, IdProvider idProvider) {
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
    protected void onEntitySaved(User user, DeviceProfile savedDeviceProfile, DeviceProfile oldDeviceProfile) throws ThingsboardException {
        clusterService.onDeviceProfileChange(savedDeviceProfile, null);
        clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedDeviceProfile.getId(),
                oldDeviceProfile == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        otaPackageStateService.update(savedDeviceProfile,
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getFirmwareId(), savedDeviceProfile.getFirmwareId()),
                oldDeviceProfile != null && !Objects.equals(oldDeviceProfile.getSoftwareId(), savedDeviceProfile.getSoftwareId()));
        entityNotificationService.notifyCreateOrUpdateOrDelete(savedDeviceProfile.getTenantId(), null,
                savedDeviceProfile.getId(), savedDeviceProfile, user, oldDeviceProfile == null ? ActionType.ADDED : ActionType.UPDATED, true, null);
    }

    @Override
    protected DeviceProfile deepCopy(DeviceProfile deviceProfile) {
        return new DeviceProfile(deviceProfile);
    }

    @Override
    protected void cleanupForComparison(DeviceProfile deviceProfile) {
        super.cleanupForComparison(deviceProfile);
        deviceProfile.setFirmwareId(null);
        deviceProfile.setSoftwareId(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
