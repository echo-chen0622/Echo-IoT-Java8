package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetInfo;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseAssetServiceTest extends AbstractServiceTest {

    private final IdComparator<Asset> idComparator = new IdComparator<>();

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
    public void testSaveAsset() {
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(asset.getTenantId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(ModelConstants.NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        savedAsset.setName("My new asset");

        assetService.saveAsset(savedAsset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());

        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithEmptyName() {
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithEmptyTenant() {
        @NotNull Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithInvalidTenant() {
        @NotNull Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignAssetToNonExistentCustomer() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        asset = assetService.saveAsset(asset);
        try {
            assetService.assignAssetToCustomer(tenantId, asset.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            assetService.deleteAsset(tenantId, asset.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignAssetToCustomerFromDifferentTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        asset = assetService.saveAsset(asset);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            assetService.assignAssetToCustomer(tenantId, asset.getId(), customer.getId());
        } finally {
            assetService.deleteAsset(tenantId, asset.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindAssetById() {
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        @NotNull List<Asset> assets = new ArrayList<>();
        try {
            for (int i=0;i<3;i++) {
                @NotNull Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset B"+i);
                asset.setType("typeB");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<7;i++) {
                @NotNull Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset C"+i);
                asset.setType("typeC");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<9;i++) {
                @NotNull Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset A"+i);
                asset.setType("typeA");
                assets.add(assetService.saveAsset(asset));
            }
            List<EntitySubtype> assetTypes = assetService.findAssetTypesByTenantId(tenantId).get();
            Assert.assertNotNull(assetTypes);
            Assert.assertEquals(3, assetTypes.size());
            Assert.assertEquals("typeA", assetTypes.get(0).getType());
            Assert.assertEquals("typeB", assetTypes.get(1).getType());
            Assert.assertEquals("typeC", assetTypes.get(2).getType());
        } finally {
            assets.forEach((asset) -> { assetService.deleteAsset(tenantId, asset.getId()); });
        }
    }

    @Test
    public void testDeleteAsset() {
        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
        foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNull(foundAsset);
    }

    @Test
    public void testFindAssetsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<Asset> assets = new ArrayList<>();
        for (int i=0;i<178;i++) {
            @NotNull Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            assets.add(assetService.saveAsset(asset));
        }

        @NotNull List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.deleteAssetsByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        @NotNull String title1 = "Asset title 1";
        @NotNull List<AssetInfo> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            @NotNull Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }
        @NotNull String title2 = "Asset title 2";
        @NotNull List<AssetInfo> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            @NotNull Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }

        @NotNull List<AssetInfo> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        @Nullable PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        @NotNull List<AssetInfo> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (@NotNull Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndType() {
        @NotNull String title1 = "Asset title 1";
        @NotNull String type1 = "typeA";
        @NotNull List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            @NotNull Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(assetService.saveAsset(asset));
        }
        @NotNull String title2 = "Asset title 2";
        @NotNull String type2 = "typeB";
        @NotNull List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            @NotNull Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(assetService.saveAsset(asset));
        }

        @NotNull List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        @Nullable PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        @NotNull List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (@NotNull Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull List<AssetInfo> assets = new ArrayList<>();
        for (int i=0;i<278;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assets.add(new AssetInfo(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId), customer.getTitle(), customer.isPublic(), "default"));
        }

        @NotNull List<AssetInfo> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.unassignCustomerAssets(tenantId, customerId);

        pageLink = new PageLink(33);
        pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull String title1 = "Asset title 1";
        @NotNull List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        @NotNull String title2 = "Asset title 2";
        @NotNull List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        @NotNull List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        @Nullable PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        @NotNull List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (@NotNull Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull String title1 = "Asset title 1";
        @NotNull String type1 = "typeC";
        @NotNull List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = assetService.saveAsset(asset);
            assetsType1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        @NotNull String title2 = "Asset title 2";
        @NotNull String type2 = "typeD";
        @NotNull List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric(15);
            @NotNull String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = assetService.saveAsset(asset);
            assetsType2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        @NotNull List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        @Nullable PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        @NotNull List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (@NotNull Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testCleanCacheIfAssetRenamed() {
        @NotNull String assetNameBeforeRename = StringUtils.randomAlphanumeric(15);
        @NotNull String assetNameAfterRename = StringUtils.randomAlphanumeric(15);

        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetNameBeforeRename);
        asset.setType("default");
        assetService.saveAsset(asset);

        Asset savedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        savedAsset.setName(assetNameAfterRename);
        assetService.saveAsset(savedAsset);

        Asset renamedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        Assert.assertNull("Can't find asset by name in cache if it was renamed", renamedAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test
    public void testFindAssetInfoByTenantId() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());

        Asset savedAsset = assetService.saveAsset(asset);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );

        @NotNull PageLink pageLinkWithType = new PageLink(100, 0, asset.getType());
        List<AssetInfo> assetInfosWithType = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithType).getData();

        Assert.assertFalse(assetInfosWithType.isEmpty());
        Assert.assertTrue(
                assetInfosWithType.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getType().equals(asset.getType())
                        )
        );
    }

    @Test
    public void testFindAssetInfoByTenantIdAndType() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    @Test
    public void testFindAssetInfoByTenantIdAndAssetProfileId() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        @NotNull Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        @NotNull PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        @NotNull PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getSearchText());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

}
