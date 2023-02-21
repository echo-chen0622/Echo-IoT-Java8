package org.echoiot.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ShortCustomerInfo;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DashboardImportService extends BaseEntityImportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));

    @NotNull
    private final DashboardService dashboardService;


    @Override
    protected void setOwner(TenantId tenantId, @NotNull Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
    }

    @Nullable
    @Override
    protected Dashboard findExistingEntity(@NotNull EntitiesImportCtx ctx, @NotNull Dashboard dashboard, IdProvider idProvider) {
        @Nullable Dashboard existingDashboard = super.findExistingEntity(ctx, dashboard, idProvider);
        if (existingDashboard == null && ctx.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(ctx.getTenantId(), dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @NotNull
    @Override
    protected Dashboard prepare(EntitiesImportCtx ctx, @NotNull Dashboard dashboard, Dashboard old, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceIdsRecursively(ctx, idProvider, entityAlias, Collections.emptySet(), HINTS);
        }
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceIdsRecursively(ctx, idProvider, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.singleton("id"), HINTS);
        }
        return dashboard;
    }

    @Override
    protected Dashboard saveOrUpdate(@NotNull EntitiesImportCtx ctx, @NotNull Dashboard dashboard, EntityExportData<Dashboard> exportData, @NotNull IdProvider idProvider) {
        var tenantId = ctx.getTenantId();

        Set<ShortCustomerInfo> assignedCustomers = Optional.ofNullable(dashboard.getAssignedCustomers()).orElse(Collections.emptySet()).stream()
                                                           .peek(customerInfo -> customerInfo.setCustomerId(idProvider.getInternalId(customerInfo.getCustomerId())))
                                                           .collect(Collectors.toSet());

        if (dashboard.getId() == null) {
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
            for (@NotNull ShortCustomerInfo customerInfo : assignedCustomers) {
                dashboard = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerInfo.getCustomerId());
            }
        } else {
            @NotNull Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers())
                                                                         .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            @NotNull Set<CustomerId> newAssignedCustomers = assignedCustomers.stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            @NotNull Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            for (CustomerId customerId : toUnassign) {
                assignedCustomers = dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }

            @NotNull Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            for (CustomerId customerId : toAssign) {
                assignedCustomers = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
        }
        return dashboard;
    }

    @NotNull
    @Override
    protected Dashboard deepCopy(@NotNull Dashboard dashboard) {
        return new Dashboard(dashboard);
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, EntityExportData<Dashboard> exportData, @NotNull Dashboard prepared, @NotNull Dashboard existing) {
        return super.compare(ctx, exportData, prepared, existing) || !prepared.getConfiguration().equals(existing.getConfiguration());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
