package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.echoiot.server.common.data.sync.ie.WidgetsBundleExportData;
import org.echoiot.server.common.data.widget.BaseWidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetTypeInfo;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleImportService extends BaseEntityImportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    @NotNull
    private final WidgetsBundleService widgetsBundleService;
    @NotNull
    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull WidgetsBundle widgetsBundle, IdProvider idProvider) {
        widgetsBundle.setTenantId(tenantId);
    }

    @Override
    protected WidgetsBundle prepare(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundle old, WidgetsBundleExportData exportData, IdProvider idProvider) {
        return widgetsBundle;
    }

    @Override
    protected WidgetsBundle saveOrUpdate(@NotNull EntitiesImportCtx ctx, @NotNull WidgetsBundle widgetsBundle, @NotNull WidgetsBundleExportData exportData, IdProvider idProvider) {
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        if (widgetsBundle.getId() == null) {
            for (@NotNull WidgetTypeDetails widget : exportData.getWidgets()) {
                widget.setId(null);
                widget.setTenantId(ctx.getTenantId());
                widget.setBundleAlias(savedWidgetsBundle.getAlias());
                widgetTypeService.saveWidgetType(widget);
            }
        } else {
            @NotNull Map<String, WidgetTypeInfo> existingWidgets = widgetTypeService.findWidgetTypesInfosByTenantIdAndBundleAlias(ctx.getTenantId(), savedWidgetsBundle.getAlias()).stream()
                                                                                    .collect(Collectors.toMap(BaseWidgetType::getAlias, w -> w));
            for (@NotNull WidgetTypeDetails widget : exportData.getWidgets()) {
                WidgetTypeInfo existingWidget;
                if ((existingWidget = existingWidgets.remove(widget.getAlias())) != null) {
                    widget.setId(existingWidget.getId());
                    widget.setCreatedTime(existingWidget.getCreatedTime());
                } else {
                    widget.setId(null);
                }
                widget.setTenantId(ctx.getTenantId());
                widget.setBundleAlias(savedWidgetsBundle.getAlias());
                widgetTypeService.saveWidgetType(widget);
            }
            existingWidgets.values().stream()
                    .map(BaseWidgetType::getId)
                    .forEach(widgetTypeId -> widgetTypeService.deleteWidgetType(ctx.getTenantId(), widgetTypeId));
        }
        return savedWidgetsBundle;
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, WidgetsBundleExportData exportData, WidgetsBundle prepared, WidgetsBundle existing) {
        return true;
    }

    @Override
    protected void onEntitySaved(@NotNull User user, @NotNull WidgetsBundle savedWidgetsBundle, @Nullable WidgetsBundle oldWidgetsBundle) throws EchoiotException {
        entityNotificationService.notifySendMsgToEdgeService(user.getTenantId(), savedWidgetsBundle.getId(),
                oldWidgetsBundle == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
    }

    @NotNull
    @Override
    protected WidgetsBundle deepCopy(@NotNull WidgetsBundle widgetsBundle) {
        return new WidgetsBundle(widgetsBundle);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}
