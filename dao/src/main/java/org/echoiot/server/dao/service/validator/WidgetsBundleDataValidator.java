package org.echoiot.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.widget.WidgetsBundleDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class WidgetsBundleDataValidator extends DataValidator<WidgetsBundle> {

    @NotNull
    private final WidgetsBundleDao widgetsBundleDao;
    @NotNull
    private final TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull WidgetsBundle widgetsBundle) {
        if (StringUtils.isEmpty(widgetsBundle.getTitle())) {
            throw new DataValidationException("Widgets bundle title should be specified!");
        }
        if (widgetsBundle.getTenantId() == null) {
            widgetsBundle.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(widgetsBundle.getTenantId())) {
                throw new DataValidationException("Widgets bundle is referencing to non-existent tenant!");
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull WidgetsBundle widgetsBundle) {
        String alias = widgetsBundle.getAlias();
        if (alias == null || alias.trim().isEmpty()) {
            alias = widgetsBundle.getTitle().toLowerCase().replaceAll("\\W+", "_");
        }
        @NotNull String originalAlias = alias;
        int c = 1;
        WidgetsBundle withSameAlias;
        do {
            withSameAlias = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(widgetsBundle.getTenantId().getId(), alias);
            if (withSameAlias != null) {
                alias = originalAlias + (++c);
            }
        } while (withSameAlias != null);
        widgetsBundle.setAlias(alias);
    }

    @NotNull
    @Override
    protected WidgetsBundle validateUpdate(TenantId tenantId, @NotNull WidgetsBundle widgetsBundle) {
        WidgetsBundle storedWidgetsBundle = widgetsBundleDao.findById(tenantId, widgetsBundle.getId().getId());
        if (!storedWidgetsBundle.getTenantId().getId().equals(widgetsBundle.getTenantId().getId())) {
            throw new DataValidationException("Can't move existing widgets bundle to different tenant!");
        }
        if (!storedWidgetsBundle.getAlias().equals(widgetsBundle.getAlias())) {
            throw new DataValidationException("Update of widgets bundle alias is prohibited!");
        }
        return storedWidgetsBundle;
    }
}
