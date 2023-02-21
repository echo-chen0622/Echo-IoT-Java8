package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class ComponentDescriptorDataValidator extends DataValidator<ComponentDescriptor> {

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull ComponentDescriptor plugin) {
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
