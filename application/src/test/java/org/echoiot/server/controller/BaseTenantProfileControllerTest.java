package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.queue.ProcessingStrategy;
import org.echoiot.server.common.data.queue.ProcessingStrategyType;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.echoiot.server.common.data.queue.SubmitStrategyType;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseTenantProfileControllerTest extends AbstractControllerTest {

    private final IdComparator<TenantProfile> idComparator = new IdComparator<>();
    private final IdComparator<EntityInfo> tenantProfileInfoIdComparator = new IdComparator<>();

    @Test
    public void testSaveTenantProfile() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
        Assert.assertNotNull(savedTenantProfile.getId());
        Assert.assertTrue(savedTenantProfile.getCreatedTime() > 0);
        Assert.assertEquals(tenantProfile.getName(), savedTenantProfile.getName());
        Assert.assertEquals(tenantProfile.getDescription(), savedTenantProfile.getDescription());
        Assert.assertEquals(tenantProfile.getProfileData(), savedTenantProfile.getProfileData());
        Assert.assertEquals(tenantProfile.isDefault(), savedTenantProfile.isDefault());
        Assert.assertEquals(tenantProfile.isIsolatedTbRuleEngine(), savedTenantProfile.isIsolatedTbRuleEngine());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.CREATED, 1);

        savedTenantProfile.setName("New tenant profile");
        doPost("/api/tenantProfile", savedTenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertEquals(foundTenantProfile.getName(), savedTenantProfile.getName());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.UPDATED, 1);
    }

    @Test
    public void testSaveTenantProfileWithViolationOfLengthValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        @NotNull TenantProfile tenantProfile = this.createTenantProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/tenantProfile", tenantProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgErrorFieldLength("name"))));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testFindTenantProfileById() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertNotNull(foundTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundTenantProfile);
    }

    @Test
    public void testFindTenantProfileInfoById() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        EntityInfo foundTenantProfileInfo = doGet("/api/tenantProfileInfo/"+savedTenantProfile.getId().getId().toString(), EntityInfo.class);
        Assert.assertNotNull(foundTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundTenantProfileInfo.getName());
    }

    @Test
    public void testFindDefaultTenantProfileInfo() throws Exception {
        loginSysAdmin();
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals("Default", foundDefaultTenantProfile.getName());
    }

    @Test
    public void testSetDefaultTenantProfile() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile 1");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile defaultTenantProfile = doPost("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString()+"/default", TenantProfile.class);
        Assert.assertNotNull(defaultTenantProfile);
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals(savedTenantProfile.getName(), foundDefaultTenantProfile.getName());
        Assert.assertEquals(savedTenantProfile.getId(), foundDefaultTenantProfile.getId());
    }

    @Test
    public void testSaveTenantProfileWithEmptyName() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        @NotNull TenantProfile tenantProfile = new TenantProfile();
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isBadRequest())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile name " + msgErrorShouldBeSpecified)));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testSaveTenantProfileWithSameName() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isOk());

        Mockito.reset(tbClusterService);

        @NotNull TenantProfile tenantProfile2 = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile with such name already exists")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testSaveSameTenantProfileWithDifferentIsolatedTbRuleEngine() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        savedTenantProfile.setIsolatedTbRuleEngine(true);
        addMainQueueConfig(savedTenantProfile);

        Mockito.reset(tbClusterService);

        doPost("/api/tenantProfile", savedTenantProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't update isolatedTbRuleEngine property")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    public void testDeleteTenantProfileWithExistingTenant() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with tenant profile");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The tenant profile referenced by the tenants cannot be deleted")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteTenantProfile() throws Exception {
        loginSysAdmin();
        @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isOk());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.DELETED, 1);

        doGet("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Tenant profile", savedTenantProfile.getId().getId().toString()))));
    }

    @Test
    public void testFindTenantProfiles() throws Exception {
        loginSysAdmin();
        @NotNull List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        tenantProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService);

        int cntEntity = 28;
        for (int i=0;i<28;i++) {
            @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.CREATED, cntEntity);

        @NotNull List<TenantProfile> loadedTenantProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                    new TypeReference<>(){}, pageLink);
            loadedTenantProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfiles, idComparator);

        Assert.assertEquals(tenantProfiles, loadedTenantProfiles);

        Mockito.reset(tbClusterService);

        for (@NotNull TenantProfile tenantProfile : loadedTenantProfiles) {
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<PageData<TenantProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.DELETED, cntEntity);
    }

    @Test
    public void testFindTenantProfileInfos() throws Exception {
        loginSysAdmin();
        @NotNull List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> tenantProfilePageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(tenantProfilePageData.hasNext());
        Assert.assertEquals(1, tenantProfilePageData.getTotalElements());
        tenantProfiles.addAll(tenantProfilePageData.getData());

        for (int i=0;i<28;i++) {
            @NotNull TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        @NotNull List<EntityInfo> loadedTenantProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                    new TypeReference<PageData<EntityInfo>>(){}, pageLink);
            loadedTenantProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfileInfos, tenantProfileInfoIdComparator);

        @NotNull List<EntityInfo> tenantProfileInfos = tenantProfiles.stream().map(tenantProfile -> new EntityInfo(tenantProfile.getId(),
                                                                                                                   tenantProfile.getName())).collect(Collectors.toList());

        Assert.assertEquals(tenantProfileInfos, loadedTenantProfileInfos);

        for (@NotNull TenantProfile tenantProfile : tenantProfiles) {
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                new TypeReference<PageData<EntityInfo>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @NotNull
    private TenantProfile createTenantProfile(String name) {
        @NotNull TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        @NotNull TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);
        return tenantProfile;
    }

    private void addMainQueueConfig(@NotNull TenantProfile tenantProfile) {
        @NotNull TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        @NotNull SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        @NotNull ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        TenantProfileData profileData = tenantProfile.getProfileData();
        profileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));
        tenantProfile.setProfileData(profileData);
    }


    private void testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(@NotNull TenantProfile tenantProfile, ComponentLifecycleEvent event, int cntTime) {
        @NotNull ArgumentMatcher<TenantProfile> matcherTenantProfile = cntTime == 1 ? argument -> argument.equals(tenantProfile) :
                argument -> argument.getClass().equals(TenantProfile.class);
        if (ComponentLifecycleEvent.DELETED.equals(event)) {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantProfileDelete(Mockito.argThat( matcherTenantProfile),
                    Mockito.isNull());
            testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        } else {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantProfileChange(Mockito.argThat(matcherTenantProfile),
                    Mockito.isNull());
            TenantProfileId tenantProfileIdId = cntTime == 1 ? tenantProfile.getId() : (TenantProfileId) createEntityId_NULL_UUID(tenantProfile);
            testBroadcastEntityStateChangeEventTime(tenantProfileIdId, null,  cntTime);
        }
        Mockito.reset(tbClusterService);
    }

    private void testBroadcastEntityStateChangeEventNeverTenantProfile() {
        Mockito.verify(tbClusterService, never()).onTenantProfileChange(Mockito.any(TenantProfile.class),
                Mockito.isNull());
        testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        Mockito.reset(tbClusterService, auditLogService);
    }
}
