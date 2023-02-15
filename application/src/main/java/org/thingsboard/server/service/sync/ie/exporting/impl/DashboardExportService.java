package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.common.util.RegexUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
@TbCoreComponent
public class DashboardExportService extends BaseEntityExportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Dashboard dashboard, EntityExportData<Dashboard> exportData) {
        if (CollectionUtils.isNotEmpty(dashboard.getAssignedCustomers())) {
            dashboard.getAssignedCustomers().forEach(customerInfo -> {
                customerInfo.setCustomerId(getExternalIdOrElseInternal(ctx, customerInfo.getCustomerId()));
            });
        }
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceUuidsRecursively(ctx, entityAlias, Collections.emptySet());
        }
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceUuidsRecursively(ctx, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.singleton("id"));
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DASHBOARD);
    }

}
