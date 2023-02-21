package org.echoiot.server.dao.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetProfileInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public abstract class BaseAssetProfileServiceTest extends AbstractServiceTest {

    private final IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private final IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

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
    public void testSaveAssetProfile() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());
        savedAssetProfile.setName("New asset profile");
        assetProfileService.saveAssetProfile(savedAssetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());
    }

    @Test
    public void testFindAssetProfileById() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    public void testFindAssetProfileInfoById() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfileInfo foundAssetProfileInfo = assetProfileService.findAssetProfileInfoById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void testFindDefaultAssetProfile() {
        AssetProfile foundDefaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertNotNull(foundDefaultAssetProfile.getId());
        Assert.assertNotNull(foundDefaultAssetProfile.getName());
    }

    @Test
    public void testFindDefaultAssetProfileInfo() {
        AssetProfileInfo foundDefaultAssetProfileInfo = assetProfileService.findDefaultAssetProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
    }

    @Test
    public void testFindOrCreateAssetProfile() throws ExecutionException, InterruptedException {
        @NotNull ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, EchoiotThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            @NotNull List<ListenableFuture<AssetProfile>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 1")));
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 2")));
            }

            List<AssetProfile> assetProfiles = Futures.allAsList(futures).get();
            assetProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    @Test
    public void testSetDefaultAssetProfile() {
        @NotNull AssetProfile assetProfile1 = this.createAssetProfile(tenantId, "Asset Profile 1");
        @NotNull AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile 2");

        AssetProfile savedAssetProfile1 = assetProfileService.saveAssetProfile(assetProfile1);
        AssetProfile savedAssetProfile2 = assetProfileService.saveAssetProfile(assetProfile2);

        boolean result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile1.getId());
        Assert.assertTrue(result);
        AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile1.getId(), defaultAssetProfile.getId());
        result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile2.getId());
        Assert.assertTrue(result);
        defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile2.getId(), defaultAssetProfile.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetProfileWithEmptyName() {
        @NotNull AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfileService.saveAssetProfile(assetProfile);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetProfileWithSameName() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        assetProfileService.saveAssetProfile(assetProfile);
        @NotNull AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile");
        assetProfileService.saveAssetProfile(assetProfile2);
    }

    @Test(expected = DataValidationException.class)
    public void testDeleteAssetProfileWithExistingAsset() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());
        assetService.saveAsset(asset);
        assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
    }

    @Test
    public void testDeleteAssetProfile() {
        @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNull(foundAssetProfile);
    }

    @Test
    public void testFindAssetProfiles() {

        @NotNull List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        @NotNull List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
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
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAssetProfileInfos() {

        @NotNull List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            @NotNull AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        @NotNull List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        @NotNull List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream()
                                                                         .map(assetProfile -> new AssetProfileInfo(assetProfile.getId(),
                        assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (@NotNull AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

}
