package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseWidgetsBundleServiceTest extends AbstractServiceTest {

    private final IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

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
    public void testSaveWidgetsBundle() throws IOException {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My first widgets bundle");

        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(widgetsBundle.getTenantId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");

        widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetsBundleWithEmptyTitle() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundleService.saveWidgetsBundle(widgetsBundle);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveWidgetsBundleWithInvalidTenant() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        widgetsBundleService.saveWidgetsBundle(widgetsBundle);
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateWidgetsBundleTenant() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        try {
            widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testUpdateWidgetsBundleAlias() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setAlias("new_alias");
        try {
            widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    @Test
    public void testFindWidgetsBundleById() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, savedWidgetsBundle.getAlias());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    @Test
    public void testDeleteWidgetsBundle() {
        @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNull(foundWidgetsBundle);
    }

    @Test
    public void testFindSystemWidgetsBundlesByPageLink() {

        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);
        @NotNull List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<235;i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(19);
        @Nullable PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findSystemWidgetsBundlesByPageLink(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (@NotNull WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindSystemWidgetsBundles() {
        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        @NotNull List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<135;i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (@NotNull WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    @Test
    public void testFindTenantWidgetsBundlesByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i=0;i<127;i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            widgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        @NotNull List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(11);
        @Nullable PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        pageLink = new PageLink(15);
        pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantIdAndPageLink() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();
        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        @NotNull List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        @NotNull List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<177;i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        @NotNull List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        @Nullable PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(14);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        @NotNull List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (@NotNull WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(18);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantId() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();
        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        @NotNull List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        @NotNull List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<277;i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        @NotNull List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        @NotNull List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (@NotNull WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);

        tenantService.deleteTenant(tenantId);
    }

}
