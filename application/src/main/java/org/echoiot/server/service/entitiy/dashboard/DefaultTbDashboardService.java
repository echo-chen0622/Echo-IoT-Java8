package org.echoiot.server.service.entitiy.dashboard;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbDashboardService extends AbstractTbEntityService implements TbDashboardService {

    @NotNull
    private final DashboardService dashboardService;

    @NotNull
    @Override
    public Dashboard save(@NotNull Dashboard dashboard, User user) throws Exception {
        @NotNull ActionType actionType = dashboard.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = dashboard.getTenantId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.saveDashboard(dashboard));
            autoCommit(user, savedDashboard.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedDashboard.getId(), savedDashboard,
                    null, actionType, user);
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), dashboard, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(@NotNull Dashboard dashboard, User user) {
        DashboardId dashboardId = dashboard.getId();
        TenantId tenantId = dashboard.getTenantId();
        try {
            @Nullable List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, dashboardId);
            dashboardService.deleteDashboard(tenantId, dashboardId);
            notificationEntityService.notifyDeleteEntity(tenantId, dashboardId, dashboard, null,
                    ActionType.DELETED, relatedEdgeIds, user, dashboardId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), ActionType.DELETED, user, e, dashboardId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard assignDashboardToCustomer(@NotNull Dashboard dashboard, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        CustomerId customerId = customer.getId();
        DashboardId dashboardId = dashboard.getId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboardId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, customerId, savedDashboard,
                    actionType, user, true, dashboardId.toString(), customerId.toString(), customer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType,
                    user, e, dashboardId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard assignDashboardToPublicCustomer(@NotNull Dashboard dashboard, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboardId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, publicCustomer.getId(), savedDashboard,
                    actionType, user, false, dashboardId.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard unassignDashboardFromPublicCustomer(@NotNull Dashboard dashboard, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Dashboard savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboardId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, publicCustomer.getId(), dashboard,
                    actionType, user, false, dashboardId.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

    @NotNull
    @Override
    public Dashboard updateDashboardCustomers(@NotNull Dashboard dashboard, @NotNull Set<CustomerId> customerIds, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            @NotNull Set<CustomerId> addedCustomerIds = new HashSet<>();
            @NotNull Set<CustomerId> removedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (!dashboard.isAssignedToCustomer(customerId)) {
                    addedCustomerIds.add(customerId);
                }
            }

            Set<ShortCustomerInfo> assignedCustomers = dashboard.getAssignedCustomers();
            if (assignedCustomers != null) {
                for (@NotNull ShortCustomerInfo customerInfo : assignedCustomers) {
                    if (!customerIds.contains(customerInfo.getCustomerId())) {
                        removedCustomerIds.add(customerInfo.getCustomerId());
                    }
                }
            }

            if (addedCustomerIds.isEmpty() && removedCustomerIds.isEmpty()) {
                return dashboard;
            } else {
                @Nullable Dashboard savedDashboard = null;
                for (@NotNull CustomerId customerId : addedCustomerIds) {
                    savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboardId, customerId));
                    @Nullable ShortCustomerInfo customerInfo = savedDashboard.getAssignedCustomerInfo(customerId);
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            actionType, user, true, dashboardId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
                for (@NotNull CustomerId customerId : removedCustomerIds) {
                    @Nullable ShortCustomerInfo customerInfo = dashboard.getAssignedCustomerInfo(customerId);
                    savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboardId, customerId));
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, savedDashboard.getId(), customerId, savedDashboard,
                            ActionType.UNASSIGNED_FROM_CUSTOMER, user, true, dashboardId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

    @NotNull
    @Override
    public Dashboard addDashboardCustomers(@NotNull Dashboard dashboard, @NotNull Set<CustomerId> customerIds, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            @NotNull Set<CustomerId> addedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (!dashboard.isAssignedToCustomer(customerId)) {
                    addedCustomerIds.add(customerId);
                }
            }
            if (addedCustomerIds.isEmpty()) {
                return dashboard;
            } else {
                @Nullable Dashboard savedDashboard = null;
                for (@NotNull CustomerId customerId : addedCustomerIds) {
                    savedDashboard = checkNotNull(dashboardService.assignDashboardToCustomer(tenantId, dashboardId, customerId));
                    @Nullable ShortCustomerInfo customerInfo = savedDashboard.getAssignedCustomerInfo(customerId);
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, customerId, savedDashboard,
                            actionType, user, true, dashboardId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard removeDashboardCustomers(@NotNull Dashboard dashboard, @NotNull Set<CustomerId> customerIds, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            @NotNull Set<CustomerId> removedCustomerIds = new HashSet<>();
            for (CustomerId customerId : customerIds) {
                if (dashboard.isAssignedToCustomer(customerId)) {
                    removedCustomerIds.add(customerId);
                }
            }
            if (removedCustomerIds.isEmpty()) {
                return dashboard;
            } else {
                @Nullable Dashboard savedDashboard = null;
                for (@NotNull CustomerId customerId : removedCustomerIds) {
                    @Nullable ShortCustomerInfo customerInfo = dashboard.getAssignedCustomerInfo(customerId);
                    savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboardId, customerId));
                    notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, customerId, savedDashboard,
                            actionType, user, true, dashboardId.toString(), customerId.toString(), customerInfo.getTitle());
                }
                return savedDashboard;
            }
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard asignDashboardToEdge(TenantId tenantId, @NotNull DashboardId dashboardId, @NotNull Edge edge, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.assignDashboardToEdge(tenantId, dashboardId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, dashboardId, null,
                    edgeId, savedDashboard, actionType, user, dashboardId.toString(),
                    edgeId.toString(), edge.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    actionType, user, e, dashboardId.toString(), edgeId);
            throw e;
        }
    }

    @Override
    public Dashboard unassignDashboardFromEdge(@NotNull Dashboard dashboard, @NotNull Edge edge, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        EdgeId edgeId = edge.getId();
        try {
            Dashboard savedDevice = checkNotNull(dashboardService.unassignDashboardFromEdge(tenantId, dashboardId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, dashboardId, null,
                    edgeId, dashboard, actionType, user, dashboardId.toString(),
                    edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e,
                    dashboardId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(@NotNull Dashboard dashboard, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = dashboard.getTenantId();
        DashboardId dashboardId = dashboard.getId();
        try {
            Dashboard savedDashboard = checkNotNull(dashboardService.unassignDashboardFromCustomer(tenantId, dashboardId, customer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, dashboardId, customer.getId(), savedDashboard,
                    actionType, user, true, dashboardId.toString(), customer.getId().toString(), customer.getName());
            return savedDashboard;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, dashboardId.toString());
            throw e;
        }
    }

}
