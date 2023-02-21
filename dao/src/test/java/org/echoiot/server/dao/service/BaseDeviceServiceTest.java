package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceInfo;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseDeviceServiceTest extends AbstractServiceTest {

    private final IdComparator<Device> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private TenantId anotherTenantId;

    @Before
    public void before() {
        tenantId = createTenant();
        anotherTenantId = createTenant();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantService.deleteTenant(anotherTenantId);

        tenantProfileService.deleteTenantProfiles(tenantId);
        tenantProfileService.deleteTenantProfiles(anotherTenantId);
    }

    @NotNull
    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSaveDevicesWithoutMaxDeviceLimit() {
        @NotNull Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    @Test
    public void testSaveDevicesWithInfiniteMaxDeviceLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(Long.MAX_VALUE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        @NotNull Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDevicesWithMaxDeviceOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(1).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));

        this.saveDevice(tenantId, "My first device");
        Assert.assertEquals(1, deviceService.countByTenantId(tenantId));

        this.saveDevice(tenantId, "My second device that out of maxDeviceCount limit");
    }

    @Test
    public void testCountByTenantId() {
        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        @NotNull Device anotherDevice = this.saveDevice(anotherTenantId, "My device 1");
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));

        int maxDevices = 8;
        @NotNull List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
            Assert.assertEquals(i, deviceService.countByTenantId(tenantId));
        }

        Assert.assertEquals(maxDevices, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        devices.forEach(device -> deleteDevice(tenantId, device));
        deleteDevice(anotherTenantId, anotherDevice);
    }

    void deleteDevice(TenantId tenantId, @NotNull Device device) {
        deviceService.deleteDevice(tenantId, device.getId());
    }

    @NotNull
    Device saveDevice(TenantId tenantId, final String name) {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(device.getTenantId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(ModelConstants.NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());

        savedDevice.setName("New " + savedDevice.getName());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
        return foundDevice;
    }

    @Test
    public void testSaveDeviceWithFirmware() {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(device.getTenantId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(ModelConstants.NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());


        @NotNull OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(device.getDeviceProfileId());
        firmware.setType(OtaPackageType.FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    @Test
    public void testAssignFirmwareToDeviceWithDifferentDeviceProfile() {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);

        @NotNull DeviceProfile deviceProfile = createDeviceProfile(tenantId, "New device Profile");
        DeviceProfile savedProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedProfile);

        @NotNull OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedProfile.getId());
        firmware.setType(OtaPackageType.FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Can't assign firmware with different deviceProfile!");
        deviceService.saveDevice(savedDevice);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithEmptyName() {
        @NotNull Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        deviceService.saveDevice(device);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithEmptyTenant() {
        @NotNull Device device = new Device();
        device.setName("My device");
        device.setType("default");
        deviceService.saveDevice(device);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithInvalidTenant() {
        @NotNull Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        deviceService.saveDevice(device);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDeviceToNonExistentCustomer() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        try {
            deviceService.assignDeviceToCustomer(tenantId, device.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDeviceToCustomerFromDifferentTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            deviceService.assignDeviceToCustomer(tenantId, device.getId(), customer.getId());
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindDeviceById() {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        Assert.assertEquals(savedDevice, foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    @Test
    public void testFindDeviceTypesByTenantId() throws Exception {
        @NotNull List<Device> devices = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                @NotNull Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device B" + i);
                device.setType("typeB");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 7; i++) {
                @NotNull Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device C" + i);
                device.setType("typeC");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 9; i++) {
                @NotNull Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device A" + i);
                device.setType("typeA");
                devices.add(deviceService.saveDevice(device));
            }
            List<EntitySubtype> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId).get();
            Assert.assertNotNull(deviceTypes);
            Assert.assertEquals(3, deviceTypes.size());
            Assert.assertEquals("typeA", deviceTypes.get(0).getType());
            Assert.assertEquals("typeB", deviceTypes.get(1).getType());
            Assert.assertEquals("typeC", deviceTypes.get(2).getType());
        } finally {
            devices.forEach((device) -> {
                deviceService.deleteDevice(tenantId, device.getId());
            });
        }
    }

    @Test
    public void testDeleteDevice() {
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNull(foundDevice);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(foundDeviceCredentials);
    }

    @Test
    public void testFindDevicesByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
        }

        @NotNull List<Device> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);

        Assert.assertEquals(devices, loadedDevices);

        deviceService.deleteDevicesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindDevicesByTenantIdAndName() {
        @NotNull String title1 = "Device title 1";
        @NotNull List<DeviceInfo> devicesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle1.add(new DeviceInfo(deviceService.saveDevice(device), null, false, "default"));
        }
        @NotNull String title2 = "Device title 2";
        @NotNull List<DeviceInfo> devicesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle2.add(new DeviceInfo(deviceService.saveDevice(device), null, false, "default"));
        }

        @NotNull List<DeviceInfo> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        @Nullable PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);

        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);

        @NotNull List<DeviceInfo> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);

        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (@NotNull Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindDevicesByTenantIdAndType() {
        @NotNull String title1 = "Device title 1";
        @NotNull String type1 = "typeA";
        @NotNull List<Device> devicesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            devicesType1.add(deviceService.saveDevice(device));
        }
        @NotNull String title2 = "Device title 2";
        @NotNull String type2 = "typeB";
        @NotNull List<Device> devicesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            devicesType2.add(deviceService.saveDevice(device));
        }

        @NotNull List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        @Nullable PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        @NotNull List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (@NotNull Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindDevicesByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull List<DeviceInfo> devices = new ArrayList<>();
        for (int i = 0; i < 278; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devices.add(new DeviceInfo(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId), customer.getTitle(), customer.isPublic(), "default"));
        }

        @NotNull List<DeviceInfo> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);

        Assert.assertEquals(devices, loadedDevices);

        deviceService.unassignCustomerDevices(tenantId, customerId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDeviceInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindDevicesByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull String title1 = "Device title 1";
        @NotNull List<Device> devicesTitle1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        @NotNull String title2 = "Device title 2";
        @NotNull List<Device> devicesTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }

        @NotNull List<Device> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        @Nullable PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);

        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);

        @NotNull List<Device> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);

        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (@NotNull Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testFindDevicesByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull String title1 = "Device title 1";
        @NotNull String type1 = "typeC";
        @NotNull List<Device> devicesType1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            device = deviceService.saveDevice(device);
            devicesType1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        @NotNull String title2 = "Device title 2";
        @NotNull String type2 = "typeD";
        @NotNull List<Device> devicesType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            device = deviceService.saveDevice(device);
            devicesType2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }

        @NotNull List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        @Nullable PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        @NotNull List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (@NotNull Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testCleanCacheIfDeviceRenamed() {
        @NotNull String deviceNameBeforeRename = StringUtils.randomAlphanumeric(15);
        @NotNull String deviceNameAfterRename = StringUtils.randomAlphanumeric(15);

        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(deviceNameBeforeRename);
        device.setType("default");
        deviceService.saveDevice(device);

        Device savedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        savedDevice.setName(deviceNameAfterRename);
        deviceService.saveDevice(savedDevice);

        Device renamedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        Assert.assertNull("Can't find device by name in cache if it was renamed", renamedDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    @Test
    public void testFindDeviceInfoByTenantId() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setType("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());

        Device savedDevice = deviceService.saveDevice(device);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByTenantId(tenantId, pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                    && d.getTenantId().equals(tenantId)
                                    && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByTenantId(tenantId, pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );

        @NotNull PageLink pageLinkWithType = new PageLink(100, 0, device.getType());
        List<DeviceInfo> deviceInfosWithType = deviceService
                .findDeviceInfosByTenantId(tenantId, pageLinkWithType).getData();

        Assert.assertFalse(deviceInfosWithType.isEmpty());
        Assert.assertTrue(
                deviceInfosWithType.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getType().equals(device.getType())
                        )
        );
    }

    @Test
    public void testFindDeviceInfoByTenantIdAndType() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setType("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());
        Device savedDevice = deviceService.saveDevice(device);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByTenantIdAndType(tenantId, device.getType(), pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                    && d.getTenantId().equals(tenantId)
                                    && d.getDeviceProfileName().equals(savedDevice.getType())
                                    && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByTenantIdAndType(tenantId, device.getType(), pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileName().equals(savedDevice.getType())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    @Test
    public void testFindDeviceInfoByTenantIdAndDeviceProfileId() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());
        Device savedDevice = deviceService.saveDevice(device);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByTenantIdAndDeviceProfileId(tenantId, savedDevice.getDeviceProfileId(), pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileId().equals(savedDevice.getDeviceProfileId())
                                        && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByTenantIdAndDeviceProfileId(tenantId, savedDevice.getDeviceProfileId(), pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileId().equals(savedDevice.getDeviceProfileId())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }
}
