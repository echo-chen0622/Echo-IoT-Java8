package org.echoiot.server.service.entitiy.dashboard;

import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.service.entitiy.SimpleTbEntityService;

import java.util.Set;

public interface TbDashboardService extends SimpleTbEntityService<Dashboard> {

    Dashboard assignDashboardToCustomer(Dashboard dashboard, Customer customer, User user) throws EchoiotException;

    Dashboard assignDashboardToPublicCustomer(Dashboard dashboard, User user) throws EchoiotException;

    Dashboard unassignDashboardFromPublicCustomer(Dashboard dashboard, User user) throws EchoiotException;

    Dashboard updateDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws EchoiotException;

    Dashboard addDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws EchoiotException;

    Dashboard removeDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws EchoiotException;

    Dashboard asignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, Edge edge, User user) throws EchoiotException;

    Dashboard unassignDashboardFromEdge(Dashboard dashboard, Edge edge, User user) throws EchoiotException;

    Dashboard unassignDashboardFromCustomer(Dashboard dashboard, Customer customer, User user) throws EchoiotException;

}
