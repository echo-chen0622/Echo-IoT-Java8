package org.echoiot.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.DeviceSearchQuery;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.device.provision.ProvisionRequest;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    DeviceInfo findDeviceInfoById(TenantId tenantId, DeviceId deviceId);

    Device findDeviceById(TenantId tenantId, DeviceId deviceId);

    ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId);

    Device findDeviceByTenantIdAndName(TenantId tenantId, String name);

    Device saveDevice(Device device, boolean doValidate);

    Device saveDevice(Device device);

    Device saveDeviceWithAccessToken(Device device, String accessToken);

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials);

    Device saveDevice(ProvisionRequest provisionRequest, DeviceProfile profile);

    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId);

    Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId);

    void deleteDevice(TenantId tenantId, DeviceId deviceId);

    PageData<Device> findDevicesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DeviceInfo> findDeviceInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type, PageLink pageLink);

    Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType);

    PageData<DeviceInfo> findDeviceInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    PageData<DeviceInfo> findDeviceInfosByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink);

    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds);

    List<Device> findDevicesByIds(List<DeviceId> deviceIds);

    ListenableFuture<List<Device>> findDevicesByIdsAsync(List<DeviceId> deviceIds);

    void deleteDevicesByTenantId(TenantId tenantId);

    PageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    PageData<DeviceInfo> findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, PageLink pageLink);

    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds);

    void unassignCustomerDevices(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query);

    ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId);

    Device assignDeviceToTenant(TenantId tenantId, Device device);

    PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink);

    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId);

    Device unassignDeviceFromEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId);

    PageData<Device> findDevicesByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);

    long countByTenantId(TenantId tenantId);
}
