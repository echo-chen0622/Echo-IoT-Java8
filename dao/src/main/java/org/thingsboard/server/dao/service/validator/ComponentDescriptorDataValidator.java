package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class ComponentDescriptorDataValidator extends DataValidator<ComponentDescriptor> {

    @Override
    protected void validateDataImpl(TenantId tenantId, ComponentDescriptor plugin) {
        if (plugin.getType() == null) {
            throw new DataValidationException("Component type should be specified!");
        }
        if (plugin.getScope() == null) {
            throw new DataValidationException("Component scope should be specified!");
        }
        if (StringUtils.isEmpty(plugin.getName())) {
            throw new DataValidationException("Component name should be specified!");
        }
        if (StringUtils.isEmpty(plugin.getClazz())) {
            throw new DataValidationException("Component clazz should be specified!");
        }
    }
}
