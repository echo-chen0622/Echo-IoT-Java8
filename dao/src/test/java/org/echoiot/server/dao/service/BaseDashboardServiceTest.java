package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.dao.exception.DataValidationException;
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
import java.util.concurrent.ExecutionException;

public abstract class BaseDashboardServiceTest extends AbstractServiceTest {

    private final IdComparator<DashboardInfo> idComparator = new IdComparator<>();

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
    public void testSaveDashboard() throws IOException {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);

        Assert.assertNotNull(savedDashboard);
        Assert.assertNotNull(savedDashboard.getId());
        Assert.assertTrue(savedDashboard.getCreatedTime() > 0);
        Assert.assertEquals(dashboard.getTenantId(), savedDashboard.getTenantId());
        Assert.assertEquals(dashboard.getTitle(), savedDashboard.getTitle());

        savedDashboard.setTitle("My new dashboard");

        dashboardService.saveDashboard(savedDashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertEquals(foundDashboard.getTitle(), savedDashboard.getTitle());

        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithEmptyTitle() {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboardService.saveDashboard(dashboard);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithEmptyTenant() {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboardService.saveDashboard(dashboard);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveDashboardWithInvalidTenant() {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        dashboardService.saveDashboard(dashboard);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToNonExistentCustomer() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        try {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToCustomerFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [dashboard]");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customer.getId());
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindDashboardById() {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        Assert.assertEquals(savedDashboard, foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }

    @Test
    public void testDeleteDashboard() {
        @NotNull Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
        foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNull(foundDashboard);
    }

    @Test
    public void testFindDashboardsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<165;i++) {
            @NotNull Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboards.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }

        @NotNull List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        @Nullable PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);

        Assert.assertEquals(dashboards, loadedDashboards);

        dashboardService.deleteDashboardsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindMobileDashboardsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<DashboardInfo> mobileDashboards = new ArrayList<>();
        for (int i=0;i<165;i++) {
            @NotNull Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboard.setMobileHide(i % 2 == 0);
            if (!dashboard.isMobileHide()) {
                dashboard.setMobileOrder(i % 4 == 0 ? (int)(Math.random() * 100) : null);
            }
            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
            if (!dashboard.isMobileHide()) {
                mobileDashboards.add(new DashboardInfo(savedDashboard));
            }
        }

        @NotNull List<DashboardInfo> loadedMobileDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(16, 0, null, new SortOrder("title", SortOrder.Direction.ASC));
        @Nullable PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink);
            loadedMobileDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(mobileDashboards, (o1, o2) -> {
            Integer order1 = o1.getMobileOrder();
            Integer order2 = o2.getMobileOrder();
            if (order1 == null && order2 == null) {
                return o1.getTitle().compareTo(o2.getTitle());
            } else if (order1 == null && order2 != null) {
                return 1;
            }  else if (order2 == null) {
                return -1;
            } else {
                return order1 - order2;
            }
        });

        Assert.assertEquals(mobileDashboards, loadedMobileDashboards);

        dashboardService.deleteDashboardsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindDashboardsByTenantIdAndTitle() {
        @NotNull String title1 = "Dashboard title 1";
        @NotNull List<DashboardInfo> dashboardsTitle1 = new ArrayList<>();
        for (int i=0;i<123;i++) {
            @NotNull Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int)(Math.random() * 17));
            @NotNull String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle1.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        @NotNull String title2 = "Dashboard title 2";
        @NotNull List<DashboardInfo> dashboardsTitle2 = new ArrayList<>();
        for (int i=0;i<193;i++) {
            @NotNull Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int)(Math.random() * 15));
            @NotNull String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle2.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }

        @NotNull List<DashboardInfo> loadedDashboardsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, title1);
        @Nullable PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboardsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle1, idComparator);
        Collections.sort(loadedDashboardsTitle1, idComparator);

        Assert.assertEquals(dashboardsTitle1, loadedDashboardsTitle1);

        @NotNull List<DashboardInfo> loadedDashboardsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboardsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle2, idComparator);
        Collections.sort(loadedDashboardsTitle2, idComparator);

        Assert.assertEquals(dashboardsTitle2, loadedDashboardsTitle2);

        for (@NotNull DashboardInfo dashboard : loadedDashboardsTitle1) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull DashboardInfo dashboard : loadedDashboardsTitle2) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindDashboardsByTenantIdAndCustomerId() throws ExecutionException, InterruptedException {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        @NotNull List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<223;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboard = dashboardService.saveDashboard(dashboard);
            dashboards.add(new DashboardInfo(dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId)));
        }

        @NotNull List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);

        Assert.assertEquals(dashboards, loadedDashboards);

        dashboardService.unassignCustomerDashboards(tenantId, customerId);

        pageLink = new PageLink(42);
        pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToNonExistentEdge() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        try {
            dashboardService.assignDashboardToEdge(tenantId, dashboard.getId(), new EdgeId(Uuids.timeBased()));
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignDashboardToEdgeFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        dashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [edge]");
        tenant = tenantService.saveTenant(tenant);
        Edge edge = new Edge();
        edge.setTenantId(tenant.getId());
        edge.setType("default");
        edge.setName("Test different edge");
        edge.setType("default");
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        edge = edgeService.saveEdge(edge);
        try {
            dashboardService.assignDashboardToEdge(tenantId, dashboard.getId(), edge.getId());
        } finally {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
}
