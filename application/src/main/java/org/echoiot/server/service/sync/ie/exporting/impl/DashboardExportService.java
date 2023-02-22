package org.echoiot.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.CollectionUtils;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

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
