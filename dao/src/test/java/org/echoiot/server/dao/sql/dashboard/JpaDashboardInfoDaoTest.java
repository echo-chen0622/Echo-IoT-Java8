package org.echoiot.server.dao.sql.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.DashboardInfo;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.AbstractJpaDaoTest;
import org.echoiot.server.dao.dashboard.DashboardInfoDao;
import org.echoiot.server.dao.service.AbstractServiceTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDashboardInfoDaoTest extends AbstractJpaDaoTest {

    @Resource
    private DashboardInfoDao dashboardInfoDao;

    @Test
    public void testFindDashboardsByTenantId() {
        @NotNull UUID tenantId1 = Uuids.timeBased();
        @NotNull UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createDashboard(tenantId1, i);
            createDashboard(tenantId2, i * 2);
        }

        @NotNull PageLink pageLink = new PageLink(15, 0, "DASHBOARD");
        PageData<DashboardInfo> dashboardInfos1 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, dashboardInfos1.getData().size());

        PageData<DashboardInfo> dashboardInfos2 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, dashboardInfos2.getData().size());
    }

    private void createDashboard(UUID tenantId, int index) {
        @NotNull DashboardInfo dashboardInfo = new DashboardInfo();
        dashboardInfo.setId(new DashboardId(Uuids.timeBased()));
        dashboardInfo.setTenantId(TenantId.fromUUID(tenantId));
        dashboardInfo.setTitle("DASHBOARD_" + index);
        dashboardInfoDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, dashboardInfo);
    }
}
