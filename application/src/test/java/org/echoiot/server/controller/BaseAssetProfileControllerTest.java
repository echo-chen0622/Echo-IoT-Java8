package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetProfileInfo;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.asset.AssetProfileDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {BaseAssetProfileControllerTest.Config.class})
public abstract class BaseAssetProfileControllerTest extends AbstractControllerTest {

    private final IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private final IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Resource
    private AssetProfileDao assetProfileDao;

    static class Config {
        @Bean
        @Primary
        public AssetProfileDao assetProfileDao(AssetProfileDao assetProfileDao) {
            return Mockito.mock(AssetProfileDao.class, AdditionalAnswers.delegatesTo(assetProfileDao));
        }
    }

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
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAssetProfile() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedAssetProfile.setName("New asset profile");
        doPost("/api/assetProfile", savedAssetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(foundAssetProfile, foundAssetProfile.getId(), foundAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveAssetProfileWithViolationOfValidation() throws Exception {
        @NotNull String msgError = msgErrorFieldLength("name");

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull AssetProfile createAssetProfile = this.createAssetProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/assetProfile", createAssetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(createAssetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindAssetProfileById() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    public void whenGetAssetProfileById_thenPermissionsAreChecked() throws Exception {
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();

        doGet("/api/assetProfile/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindAssetProfileInfoById() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfileInfo foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());

        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        @NotNull User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer2@echoiot.org");

        createUserAndLogin(customerUser, "customer");

        foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void whenGetAssetProfileInfoById_thenPermissionsAreChecked() throws Exception {
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();
        doGet("/api/assetProfileInfo/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindDefaultAssetProfileInfo() throws Exception {
        AssetProfileInfo foundDefaultAssetProfileInfo = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
        Assert.assertEquals("default", foundDefaultAssetProfileInfo.getName());
    }

    @Test
    public void testSetDefaultAssetProfile() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile 1");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile defaultAssetProfile = doPost("/api/assetProfile/" + savedAssetProfile.getId().getId().toString() + "/default", AssetProfile.class);
        Assert.assertNotNull(defaultAssetProfile);
        AssetProfileInfo foundDefaultAssetProfile = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertEquals(savedAssetProfile.getName(), foundDefaultAssetProfile.getName());
        Assert.assertEquals(savedAssetProfile.getId(), foundDefaultAssetProfile.getId());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(defaultAssetProfile, defaultAssetProfile.getId(), defaultAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveAssetProfileWithEmptyName() throws Exception {
        @NotNull AssetProfile assetProfile = new AssetProfile();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull String msgError = "Asset profile name " + msgErrorShouldBeSpecified;
        doPost("/api/assetProfile", assetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveAssetProfileWithSameName() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        doPost("/api/assetProfile", assetProfile).andExpect(status().isOk());
        @NotNull AssetProfile assetProfile2 = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull String msgError = "Asset profile with such name already exists";
        doPost("/api/assetProfile", assetProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testDeleteAssetProfileWithExistingAsset() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        @NotNull Asset asset = new Asset();
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());

        doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The asset profile referenced by the assets cannot be deleted")));

        testNotifyEntityNever(savedAssetProfile.getId(), savedAssetProfile);
    }

    @Test
    public void testSaveAssetProfileWithRuleChainFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        @NotNull RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Different rule chain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        loginTenantAdmin();

        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultRuleChainId(savedRuleChain.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign rule chain from different tenant!")));
    }

    @Test
    public void testSaveAssetProfileWithDashboardFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Different dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        loginTenantAdmin();

        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultDashboardId(savedDashboard.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign dashboard from different tenant!")));
    }

    @Test
    public void testDeleteAssetProfile() throws Exception {
        @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isOk());

        String savedAssetProfileIdStr = savedAssetProfile.getId().getId().toString();
        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAssetProfileIdStr);

        doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset profile", savedAssetProfileIdStr))));
    }

    @Test
    public void testFindAssetProfiles() throws Exception {
        @NotNull List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 28;
        for (int i = 0; i < cntEntity; i++) {
            @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new AssetProfile(), new AssetProfile(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED, cntEntity, cntEntity, cntEntity);
        Mockito.reset(tbClusterService, auditLogService);

        @NotNull List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        for (@NotNull AssetProfile assetProfile : loadedAssetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(loadedAssetProfiles.get(0), loadedAssetProfiles.get(0),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, cntEntity, loadedAssetProfiles.get(0).getId().getId().toString());

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAssetProfileInfos() throws Exception {
        @NotNull List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<PageData<AssetProfile>>() {
                }, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        @NotNull List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        @NotNull List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream().map(assetProfile -> new AssetProfileInfo(assetProfile.getId(),
                                                                                                                            assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (@NotNull AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                new TypeReference<PageData<AssetProfileInfo>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testDeleteAssetProfileWithDeleteRelationsOk() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    @Test
    public void testDeleteAssetProfileExceptionWithRelationsTransactional() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetProfileDao, savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    private AssetProfile savedAssetProfile(String name) {
        @NotNull AssetProfile assetProfile = createAssetProfile(name);
        return doPost("/api/assetProfile", assetProfile, AssetProfile.class);
    }
}
