package org.echoiot.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.widget.WidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.widget.WidgetTypeDao;
import org.echoiot.server.dao.widget.WidgetsBundleDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class WidgetTypeDataValidator extends DataValidator<WidgetTypeDetails> {

    @NotNull
    private final WidgetTypeDao widgetTypeDao;
    @NotNull
    private final WidgetsBundleDao widgetsBundleDao;
    @NotNull
    private final TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull WidgetTypeDetails widgetTypeDetails) {
        if (StringUtils.isEmpty(widgetTypeDetails.getName())) {
            throw new DataValidationException("Widgets type name should be specified!");
        }
        if (StringUtils.isEmpty(widgetTypeDetails.getBundleAlias())) {
            throw new DataValidationException("Widgets type bundle alias should be specified!");
        }
        if (widgetTypeDetails.getDescriptor() == null || widgetTypeDetails.getDescriptor().size() == 0) {
            throw new DataValidationException("Widgets type descriptor can't be empty!");
        }
        if (widgetTypeDetails.getTenantId() == null) {
            widgetTypeDetails.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!widgetTypeDetails.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(widgetTypeDetails.getTenantId())) {
                throw new DataValidationException("Widget type is referencing to non-existent tenant!");
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull WidgetTypeDetails widgetTypeDetails) {
        WidgetsBundle widgetsBundle = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(widgetTypeDetails.getTenantId().getId(), widgetTypeDetails.getBundleAlias());
        if (widgetsBundle == null) {
            throw new DataValidationException("Widget type is referencing to non-existent widgets bundle!");
        }
        String alias = widgetTypeDetails.getAlias();
        if (alias == null || alias.trim().isEmpty()) {
            alias = widgetTypeDetails.getName().toLowerCase().replaceAll("\\W+", "_");
        }
        @NotNull String originalAlias = alias;
        int c = 1;
        WidgetType withSameAlias;
        do {
            withSameAlias = widgetTypeDao.findByTenantIdBundleAliasAndAlias(widgetTypeDetails.getTenantId().getId(), widgetTypeDetails.getBundleAlias(), alias);
            if (withSameAlias != null) {
                alias = originalAlias + (++c);
            }
        } while (withSameAlias != null);
        widgetTypeDetails.setAlias(alias);
    }

    @NotNull
    @Override
    protected WidgetTypeDetails validateUpdate(TenantId tenantId, @NotNull WidgetTypeDetails widgetTypeDetails) {
        WidgetTypeDetails storedWidgetType = widgetTypeDao.findById(tenantId, widgetTypeDetails.getId().getId());
        if (!storedWidgetType.getTenantId().getId().equals(widgetTypeDetails.getTenantId().getId())) {
            throw new DataValidationException("Can't move existing widget type to different tenant!");
        }
        if (!storedWidgetType.getBundleAlias().equals(widgetTypeDetails.getBundleAlias())) {
            throw new DataValidationException("Update of widget type bundle alias is prohibited!");
        }
        if (!storedWidgetType.getAlias().equals(widgetTypeDetails.getAlias())) {
            throw new DataValidationException("Update of widget type alias is prohibited!");
        }
        return new WidgetTypeDetails(storedWidgetType);
    }
}
