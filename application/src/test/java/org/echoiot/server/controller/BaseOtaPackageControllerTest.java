package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.echoiot.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseOtaPackageControllerTest extends AbstractControllerTest {

    private final IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String CHECKSUM_ALGORITHM = "SHA256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private Tenant savedTenant;
    private User tenantAdmin;
    private DeviceProfileId deviceProfileId;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveFirmware() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundFirmwareInfo, foundFirmwareInfo.getId(), foundFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveOtaPackageInfoWithViolationOfLengthValidation() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(StringUtils.randomAlphabetic(300));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);
        @NotNull String msgError = msgErrorFieldLength("title");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("version");
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(true);
        msgError = msgErrorFieldLength("url");
        firmwareInfo.setUrl(StringUtils.randomAlphabetic(300));
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveFirmwareData() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        @NotNull MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Assert.assertEquals(FILE_NAME, savedFirmware.getFileName());
        Assert.assertEquals(CONTENT_TYPE, savedFirmware.getContentType());
        Assert.assertEquals(CHECKSUM_ALGORITHM, savedFirmware.getChecksumAlgorithm().name());
        Assert.assertEquals(CHECKSUM, savedFirmware.getChecksum());

        testNotifyEntityAllOneTime(savedFirmware, savedFirmware.getId(), savedFirmware.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testUpdateFirmwareFromDifferentTenant() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage",
                new SaveOtaPackageInfoRequest(savedFirmwareInfo, false))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedFirmwareInfo.getId(), savedFirmwareInfo);

        deleteDifferentTenant();
    }

    @Test
    public void testFindFirmwareInfoById() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        OtaPackageInfo foundFirmware = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmwareInfo, foundFirmware);
    }

    @Test
    public void testFindFirmwareById() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        @NotNull MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        OtaPackage foundFirmware = doGet("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString(), OtaPackage.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        Assert.assertEquals(DATA, foundFirmware.getData());
    }

    @Test
    public void testDeleteFirmware() throws Exception {
        @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTime(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedFirmwareInfo.getId().getId().toString());

        doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));
    }

    @Test
    public void testFindTenantFirmwares() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull List<OtaPackageInfo> otaPackages = new ArrayList<>();
        int cntEntity = 165;
        int startIndexSaveData = 101;
        for (int i = 0; i < cntEntity; i++) {
            @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i >= startIndexSaveData) {
                @NotNull MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
            }
            otaPackages.add(savedFirmwareInfo);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new OtaPackageInfo(), new OtaPackageInfo(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED, cntEntity, 0, (cntEntity*2 - startIndexSaveData));

        @NotNull List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackages, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(otaPackages, loadedFirmwares);
    }

    @Test
    public void testFindTenantFirmwaresByHasData() throws Exception {
        @NotNull List<OtaPackageInfo> otaPackagesWithData = new ArrayList<>();
        @NotNull List<OtaPackageInfo> allOtaPackages = new ArrayList<>();

        for (int i = 0; i < 165; i++) {
            @NotNull SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                @NotNull MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
                otaPackagesWithData.add(savedFirmwareInfo);
            }

            allOtaPackages.add(savedFirmwareInfo);
        }

        @NotNull List<OtaPackageInfo> loadedOtaPackagesWithData = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages/" + deviceProfileId.toString() + "/FIRMWARE?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedOtaPackagesWithData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        @NotNull List<OtaPackageInfo> allLoadedOtaPackages = new ArrayList<>();
        pageLink = new PageLink(24);
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            allLoadedOtaPackages.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackagesWithData, idComparator);
        Collections.sort(allOtaPackages, idComparator);
        Collections.sort(loadedOtaPackagesWithData, idComparator);
        Collections.sort(allLoadedOtaPackages, idComparator);

        Assert.assertEquals(otaPackagesWithData, loadedOtaPackagesWithData);
        Assert.assertEquals(allOtaPackages, allLoadedOtaPackages);
    }

    private OtaPackageInfo save(SaveOtaPackageInfoRequest firmwareInfo) throws Exception {
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    protected OtaPackage savaData(@NotNull String urlTemplate, @NotNull MockMultipartFile content, String... params) throws Exception {
        @NotNull MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackage.class);
    }

}
