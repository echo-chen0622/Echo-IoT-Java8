package org.echoiot.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.common.data.sync.ie.WidgetsBundleExportData;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.List;
import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleExportService extends BaseEntityExportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    @NotNull
    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setRelatedEntities(@NotNull EntitiesExportCtx<?> ctx, @NotNull WidgetsBundle widgetsBundle, @NotNull WidgetsBundleExportData exportData) {
        if (widgetsBundle.getTenantId() == null || widgetsBundle.getTenantId().isNullUid()) {
            throw new IllegalArgumentException("Export of system Widget Bundles is not allowed");
        }

        List<WidgetTypeDetails> widgets = widgetTypeService.findWidgetTypesDetailsByTenantIdAndBundleAlias(ctx.getTenantId(), widgetsBundle.getAlias());
        exportData.setWidgets(widgets);
    }

    @NotNull
    @Override
    protected WidgetsBundleExportData newExportData() {
        return new WidgetsBundleExportData();
    }

    @NotNull
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.WIDGETS_BUNDLE);
    }

}
