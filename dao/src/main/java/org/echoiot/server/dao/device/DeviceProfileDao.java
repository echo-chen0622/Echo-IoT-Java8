package org.echoiot.server.dao.device;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceProfileInfo;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.ExportableEntityDao;

import java.util.UUID;

public interface DeviceProfileDao extends Dao<DeviceProfile>, ExportableEntityDao<DeviceProfileId, DeviceProfile> {

    DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, UUID deviceProfileId);

    DeviceProfile save(TenantId tenantId, DeviceProfile deviceProfile);

    DeviceProfile saveAndFlush(TenantId tenantId, DeviceProfile deviceProfile);

    PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink);

    PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType);

    DeviceProfile findDefaultDeviceProfile(TenantId tenantId);

    DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId);

    DeviceProfile findByProvisionDeviceKey(String provisionDeviceKey);

    DeviceProfile findByName(TenantId tenantId, String profileName);
}
