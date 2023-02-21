package org.echoiot.server.dao.sql.widget;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.widget.WidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.dao.widget.WidgetTypeDao;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    List<WidgetType> widgetTypeList;

    @Resource
    private WidgetTypeDao widgetTypeDao;

    @Before
    public void setUp() {
        widgetTypeList = new ArrayList<>();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            widgetTypeList.add(createAndSaveWidgetType(i));
        }
    }

    WidgetType createAndSaveWidgetType(int number) {
        @NotNull WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(TenantId.SYS_TENANT_ID);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setAlias("ALIAS_" + number);
        widgetType.setBundleAlias(BUNDLE_ALIAS);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    public void deleteAllWidgetType() {
        for (@NotNull WidgetType widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindByTenantIdAndBundleAlias() {
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS);
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        WidgetType result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS, "ALIAS_0");
        Assert.assertEquals(result.getId(), widgetType.getId());
    }
}
