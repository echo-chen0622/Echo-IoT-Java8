package org.echoiot.server.dao.sql.widget;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetsBundleDao;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
public class JpaWidgetsBundleDaoTest extends AbstractJpaDaoTest {

    List<WidgetsBundle> widgetsBundles;
    @Resource
    private WidgetsBundleDao widgetsBundleDao;

    @After
    public void tearDown() {
        for (@NotNull WidgetsBundle widgetsBundle : widgetsBundles) {
            widgetsBundleDao.removeById(widgetsBundle.getTenantId(), widgetsBundle.getUuidId());
        }

    }

    @Test
    public void testFindAll() {
        createSystemWidgetBundles(7, "WB_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(7, widgetsBundles.size());
    }

    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        createSystemWidgetBundles(1, "WB_");
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(
                TenantId.SYS_TENANT_ID.getId(), "WB_" + 0);
        widgetsBundles = List.of(widgetsBundle);
        assertEquals("WB_" + 0, widgetsBundle.getAlias());
    }

    @Test
    public void testFindSystemWidgetsBundles() {
        createSystemWidgetBundles(30, "WB_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        assertEquals(30, widgetsBundles.size());
        // Get first page
        PageLink pageLink = new PageLink(10, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, pageLink);
        assertEquals(10, widgetsBundles1.getData().size());
        // Get next page
        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID, pageLink);
        assertEquals(10, widgetsBundles2.getData().size());
    }

    @Test
    public void testFindWidgetsBundlesByTenantId() {
        @NotNull UUID tenantId1 = Uuids.timeBased();
        @NotNull UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i = 0; i < 10; i++) {
            createWidgetBundles(3, tenantId1, "WB1_");
            createWidgetBundles(5, tenantId2, "WB2_");
            createSystemWidgetBundles(10, "WB_SYS_");
        }
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(180, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());

        @NotNull PageLink pageLink1 = new PageLink(40, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId1, pageLink1);
        assertEquals(30, widgetsBundles1.getData().size());

        PageLink pageLink2 = new PageLink(40, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(40, widgetsBundles2.getData().size());

        pageLink2 = pageLink2.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId2, pageLink2);
        assertEquals(10, widgetsBundles3.getData().size());
    }

    @Test
    public void testFindAllWidgetsBundlesByTenantId() {
        @NotNull UUID tenantId1 = Uuids.timeBased();
        @NotNull UUID tenantId2 = Uuids.timeBased();
        // Create a bunch of widgetBundles
        for (int i = 0; i < 10; i++) {
            createWidgetBundles(5, tenantId1, "WB1_");
            createWidgetBundles(3, tenantId2, "WB2_");
            createSystemWidgetBundles(2, "WB_SYS_");
        }
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(100, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());

        PageLink pageLink = new PageLink(30, 0, "WB");
        PageData<WidgetsBundle> widgetsBundles1 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(30, widgetsBundles1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles2 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(30, widgetsBundles2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles3 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(10, widgetsBundles3.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId1, pageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    @Test
    public void testSearchTextNotFound() {
        @NotNull UUID tenantId = Uuids.timeBased();
        createWidgetBundles(5, tenantId, "ABC_");
        createSystemWidgetBundles(5, "SYS_");
        widgetsBundles = widgetsBundleDao.find(TenantId.SYS_TENANT_ID);
        Assert.assertEquals(10, widgetsBundleDao.find(TenantId.SYS_TENANT_ID).size());
        @NotNull PageLink textPageLink = new PageLink(30, 0, "TEXT_NOT_FOUND");
        PageData<WidgetsBundle> widgetsBundles4 = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId, textPageLink);
        assertEquals(0, widgetsBundles4.getData().size());
    }

    private void createWidgetBundles(int count, UUID tenantId, String prefix) {
        for (int i = 0; i < count; i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundle.setTenantId(TenantId.fromUUID(tenantId));
            widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);
        }
    }

    private void createSystemWidgetBundles(int count, String prefix) {
        for (int i = 0; i < count; i++) {
            @NotNull WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setAlias(prefix + i);
            widgetsBundle.setTitle(prefix + i);
            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
            widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
            widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);
        }
    }
}
