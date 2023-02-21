package org.echoiot.server.dao.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.assertj.core.api.Assertions;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseDeviceProfileServiceTest extends AbstractServiceTest {

    private final IdComparator<DeviceProfile> idComparator = new IdComparator<>();
    private final IdComparator<DeviceProfileInfo> deviceProfileInfoIdComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveDeviceProfile() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        Assert.assertNotNull(savedDeviceProfile.getId());
        Assert.assertTrue(savedDeviceProfile.getCreatedTime() > 0);
        Assert.assertEquals(deviceProfile.getName(), savedDeviceProfile.getName());
        Assert.assertEquals(deviceProfile.getDescription(), savedDeviceProfile.getDescription());
        Assert.assertEquals(deviceProfile.getProfileData(), savedDeviceProfile.getProfileData());
        Assert.assertEquals(deviceProfile.isDefault(), savedDeviceProfile.isDefault());
        Assert.assertEquals(deviceProfile.getDefaultRuleChainId(), savedDeviceProfile.getDefaultRuleChainId());
        savedDeviceProfile.setName("New device profile");
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testSaveDeviceProfileWithFirmware() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        Assert.assertNotNull(savedDeviceProfile.getId());
        Assert.assertTrue(savedDeviceProfile.getCreatedTime() > 0);
        Assert.assertEquals(deviceProfile.getName(), savedDeviceProfile.getName());
        Assert.assertEquals(deviceProfile.getDescription(), savedDeviceProfile.getDescription());
        Assert.assertEquals(deviceProfile.getProfileData(), savedDeviceProfile.getProfileData());
        Assert.assertEquals(deviceProfile.isDefault(), savedDeviceProfile.isDefault());
        Assert.assertEquals(deviceProfile.getDefaultRuleChainId(), savedDeviceProfile.getDefaultRuleChainId());

        @NotNull OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedDeviceProfile.getId());
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

        deviceProfile.setFirmwareId(savedFirmware.getId());

        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testFindDeviceProfileById() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertNotNull(foundDeviceProfile);
        Assert.assertEquals(savedDeviceProfile, foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfileInfoById() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfileInfo foundDeviceProfileInfo = deviceProfileService.findDeviceProfileInfoById(tenantId, savedDeviceProfile.getId());
        Assert.assertNotNull(foundDeviceProfileInfo);
        Assert.assertEquals(savedDeviceProfile.getId(), foundDeviceProfileInfo.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfileInfo.getName());
        Assert.assertEquals(savedDeviceProfile.getType(), foundDeviceProfileInfo.getType());
    }

    @Test
    public void testFindDefaultDeviceProfile() {
        DeviceProfile foundDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(foundDefaultDeviceProfile);
        Assert.assertNotNull(foundDefaultDeviceProfile.getId());
        Assert.assertNotNull(foundDefaultDeviceProfile.getName());
    }

    @Test
    public void testFindDefaultDeviceProfileInfo() {
        DeviceProfileInfo foundDefaultDeviceProfileInfo = deviceProfileService.findDefaultDeviceProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getId());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getName());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getType());
    }

    @Test
    public void testFindOrCreateDeviceProfile() throws ExecutionException, InterruptedException {
        @NotNull ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, EchoiotThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            @NotNull List<ListenableFuture<DeviceProfile>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(testExecutor.submit(() -> deviceProfileService.findOrCreateDeviceProfile(tenantId, "Device Profile 1")));
                futures.add(testExecutor.submit(() -> deviceProfileService.findOrCreateDeviceProfile(tenantId, "Device Profile 2")));
            }

            List<DeviceProfile> deviceProfiles = Futures.allAsList(futures).get();
            deviceProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void testSetDefaultDeviceProfile() {
        @NotNull DeviceProfile deviceProfile1 = this.createDeviceProfile(tenantId, "Device Profile 1");
        @NotNull DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId, "Device Profile 2");

        DeviceProfile savedDeviceProfile1 = deviceProfileService.saveDeviceProfile(deviceProfile1);
        DeviceProfile savedDeviceProfile2 = deviceProfileService.saveDeviceProfile(deviceProfile2);

        boolean result = deviceProfileService.setDefaultDeviceProfile(tenantId, savedDeviceProfile1.getId());
        Assert.assertTrue(result);
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(defaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile1.getId(), defaultDeviceProfile.getId());
        result = deviceProfileService.setDefaultDeviceProfile(tenantId, savedDeviceProfile2.getId());
        Assert.assertTrue(result);
        defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        Assert.assertNotNull(defaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile2.getId(), defaultDeviceProfile.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceProfileWithEmptyName() {
        @NotNull DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDeviceProfileWithSameName() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfileService.saveDeviceProfile(deviceProfile);
        @NotNull DeviceProfile deviceProfile2 = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfileService.saveDeviceProfile(deviceProfile2);
    }

    @Ignore
    @Test(expected = DataValidationException.class)
    public void testChangeDeviceProfileTypeWithExistingDevices() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        //TODO: once we have more profile types, we should test that we can not change profile type in runtime and uncomment the @Ignore.
//        savedDeviceProfile.setType(DeviceProfileType.LWM2M);
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testChangeDeviceProfileTransportTypeWithExistingDevices() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        savedDeviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfileService.saveDeviceProfile(savedDeviceProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteDeviceProfileWithExistingDevice() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        @NotNull Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setType("default");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        deviceService.saveDevice(device);
        deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
    }

    @Test
    public void testDeleteDeviceProfileWithExistingOta_cascadeDelete() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        OtaPackage otaPackage = constructDefaultOtaPackage(tenantId, deviceProfile.getId());
        otaPackage = otaPackageService.saveOtaPackage(otaPackage);

        assertThat(deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId())).isNotNull();
        Assertions.assertThat(otaPackageService.findOtaPackageById(tenantId, otaPackage.getId())).isNotNull();

        deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());

        assertThat(deviceProfileService.findDeviceProfileById(tenantId, deviceProfile.getId())).isNull();
        Assertions.assertThat(otaPackageService.findOtaPackageById(tenantId, otaPackage.getId())).isNull();
    }

    @Test
    public void testDeleteDeviceProfile() {
        @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        deviceProfileService.deleteDeviceProfile(tenantId, savedDeviceProfile.getId());
        DeviceProfile foundDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, savedDeviceProfile.getId());
        Assert.assertNull(foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfiles() {

        @NotNull List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        deviceProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile" + i);
            deviceProfiles.add(deviceProfileService.saveDeviceProfile(deviceProfile));
        }

        @NotNull List<DeviceProfile> loadedDeviceProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
            loadedDeviceProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfiles, idComparator);

        Assert.assertEquals(deviceProfiles, loadedDeviceProfiles);

        for (@NotNull DeviceProfile deviceProfile : loadedDeviceProfiles) {
            if (!deviceProfile.isDefault()) {
                deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindDeviceProfileInfos() {

        @NotNull List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> deviceProfilePageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
        Assert.assertFalse(deviceProfilePageData.hasNext());
        Assert.assertEquals(1, deviceProfilePageData.getTotalElements());
        deviceProfiles.addAll(deviceProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile" + i);
            deviceProfiles.add(deviceProfileService.saveDeviceProfile(deviceProfile));
        }

        @NotNull List<DeviceProfileInfo> loadedDeviceProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<DeviceProfileInfo> pageData;
        do {
            pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink, null);
            loadedDeviceProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfileInfos, deviceProfileInfoIdComparator);

        @NotNull List<DeviceProfileInfo> deviceProfileInfos = deviceProfiles.stream()
                                                                            .map(deviceProfile -> new DeviceProfileInfo(deviceProfile.getId(),
                        deviceProfile.getName(), deviceProfile.getImage(), deviceProfile.getDefaultDashboardId(),
                        deviceProfile.getType(), deviceProfile.getTransportType())).collect(Collectors.toList());

        Assert.assertEquals(deviceProfileInfos, loadedDeviceProfileInfos);

        for (@NotNull DeviceProfile deviceProfile : deviceProfiles) {
            if (!deviceProfile.isDefault()) {
                deviceProfileService.deleteDeviceProfile(tenantId, deviceProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = deviceProfileService.findDeviceProfileInfos(tenantId, pageLink, null);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

}
