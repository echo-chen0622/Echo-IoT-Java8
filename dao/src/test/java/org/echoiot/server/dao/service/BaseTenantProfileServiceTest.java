package org.echoiot.server.dao.service;

import com.fasterxml.jackson.databind.node.NullNode;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityInfo;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.ProcessingStrategy;
import org.echoiot.server.common.data.queue.ProcessingStrategyType;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.echoiot.server.common.data.queue.SubmitStrategyType;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseTenantProfileServiceTest extends AbstractServiceTest {

    private final IdComparator<TenantProfile> idComparator = new IdComparator<>();
    private final IdComparator<EntityInfo> tenantProfileInfoIdComparator = new IdComparator<>();

    @After
    public void after() {
        tenantProfileService.deleteTenantProfiles(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveTenantProfile() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");

        tenantProfile.setIsolatedTbRuleEngine(true);

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
        mainQueueConfiguration.setAdditionalInfo(NullNode.getInstance());
        tenantProfile.getProfileData().setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Assert.assertNotNull(savedTenantProfile);
        Assert.assertNotNull(savedTenantProfile.getId());
        Assert.assertTrue(savedTenantProfile.getCreatedTime() > 0);
        Assert.assertEquals(tenantProfile.getName(), savedTenantProfile.getName());
        Assert.assertEquals(tenantProfile.getDescription(), savedTenantProfile.getDescription());
        Assert.assertEquals(tenantProfile.getProfileData(), savedTenantProfile.getProfileData());
        Assert.assertEquals(tenantProfile.isDefault(), savedTenantProfile.isDefault());
        Assert.assertEquals(tenantProfile.isIsolatedTbRuleEngine(), savedTenantProfile.isIsolatedTbRuleEngine());

        savedTenantProfile.setName("New tenant profile");
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile);
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertEquals(foundTenantProfile.getName(), savedTenantProfile.getName());
    }

    @Test
    public void testFindTenantProfileById() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNotNull(foundTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundTenantProfile);
    }

    @Test
    public void testFindTenantProfileInfoById() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        EntityInfo foundTenantProfileInfo = tenantProfileService.findTenantProfileInfoById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNotNull(foundTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundTenantProfileInfo.getName());
    }

    @Test
    public void testFindDefaultTenantProfile() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Default Tenant Profile");
        tenantProfile.setDefault(true);
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        TenantProfile foundDefaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundDefaultTenantProfile);
    }

    @Test
    public void testFindDefaultTenantProfileInfo() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Default Tenant Profile");
        tenantProfile.setDefault(true);
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        EntityInfo foundDefaultTenantProfileInfo = tenantProfileService.findDefaultTenantProfileInfo(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(foundDefaultTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundDefaultTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundDefaultTenantProfileInfo.getName());
    }

    @Test
    public void testSetDefaultTenantProfile() {
        @NotNull TenantProfile tenantProfile1 = createTenantProfile("Tenant Profile 1");
        @NotNull TenantProfile tenantProfile2 = createTenantProfile("Tenant Profile 2");

        TenantProfile savedTenantProfile1 = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile1);
        TenantProfile savedTenantProfile2 = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile2);

        boolean result = tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile1.getId());
        Assert.assertTrue(result);
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(defaultTenantProfile);
        Assert.assertEquals(savedTenantProfile1.getId(), defaultTenantProfile.getId());
        result = tenantProfileService.setDefaultTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile2.getId());
        Assert.assertTrue(result);
        defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(defaultTenantProfile);
        Assert.assertEquals(savedTenantProfile2.getId(), defaultTenantProfile.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTenantProfileWithEmptyName() {
        @NotNull TenantProfile tenantProfile = new TenantProfile();
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveTenantProfileWithSameName() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        @NotNull TenantProfile tenantProfile2 = createTenantProfile("Tenant Profile");
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile2);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveSameTenantProfileWithDifferentIsolatedTbRuleEngine() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        savedTenantProfile.setIsolatedTbRuleEngine(true);
        addMainQueueConfig(savedTenantProfile);
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteTenantProfileWithExistingTenant() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        tenant = tenantService.saveTenant(tenant);
        try {
            tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        } finally {
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testDeleteTenantProfile() {
        @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        TenantProfile foundTenantProfile = tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, savedTenantProfile.getId());
        Assert.assertNull(foundTenantProfile);
    }

    @Test
    public void testFindTenantProfiles() {

        @NotNull List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenantProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile));
        }

        @NotNull List<TenantProfile> loadedTenantProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
            loadedTenantProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfiles, idComparator);

        Assert.assertEquals(tenantProfiles, loadedTenantProfiles);

        for (@NotNull TenantProfile tenantProfile : loadedTenantProfiles) {
            tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile.getId());
        }

        pageLink = new PageLink(17);
        pageData = tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindTenantProfileInfos() {

        @NotNull List<TenantProfile> tenantProfiles = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            @NotNull TenantProfile tenantProfile = createTenantProfile("Tenant Profile" + i);
            tenantProfiles.add(tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile));
        }

        @NotNull List<EntityInfo> loadedTenantProfileInfos = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = tenantProfileService.findTenantProfileInfos(TenantId.SYS_TENANT_ID, pageLink);
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

        for (@NotNull EntityInfo tenantProfile : loadedTenantProfileInfos) {
            tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, new TenantProfileId(tenantProfile.getId().getId()));
        }

        pageLink = new PageLink(17);
        pageData = tenantProfileService.findTenantProfileInfos(TenantId.SYS_TENANT_ID, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @NotNull
    public static TenantProfile createTenantProfile(String name) {
        @NotNull TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        @NotNull TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(profileData);
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
}
