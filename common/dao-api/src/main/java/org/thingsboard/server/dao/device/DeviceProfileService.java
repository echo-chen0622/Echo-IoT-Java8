package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface DeviceProfileService {

    DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName);

    DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId);

    DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile);

    void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink);

    PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType);

    DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String profileName);

    DeviceProfile createDefaultDeviceProfile(TenantId tenantId);

    DeviceProfile findDefaultDeviceProfile(TenantId tenantId);

    DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId);

    boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId);

    void deleteDeviceProfilesByTenantId(TenantId tenantId);

}
