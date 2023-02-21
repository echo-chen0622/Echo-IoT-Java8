package org.echoiot.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentScope;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.Validator;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseComponentDescriptorService implements ComponentDescriptorService {

    @Resource
    private ComponentDescriptorDao componentDescriptorDao;

    @Resource
    private DataValidator<ComponentDescriptor> componentValidator;

    @Override
    public ComponentDescriptor saveComponent(TenantId tenantId, @NotNull ComponentDescriptor component) {
        componentValidator.validate(component, data -> TenantId.SYS_TENANT_ID);
        Optional<ComponentDescriptor> result = componentDescriptorDao.saveIfNotExist(tenantId, component);
        return result.orElseGet(() -> componentDescriptorDao.findByClazz(tenantId, component.getClazz()));
    }

    @Override
    public ComponentDescriptor findById(TenantId tenantId, @NotNull ComponentDescriptorId componentId) {
        Validator.validateId(componentId, "Incorrect component id for search request.");
        return componentDescriptorDao.findById(tenantId, componentId);
    }

    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, @NotNull String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for search request.");
        return componentDescriptorDao.findByClazz(tenantId, clazz);
    }

    @Override
    public PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, @NotNull PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByTypeAndPageLink(tenantId, type, pageLink);
    }

    @Override
    public PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, @NotNull PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByScopeAndTypeAndPageLink(tenantId, scope, type, pageLink);
    }

    @Override
    public void deleteByClazz(TenantId tenantId, @NotNull String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for delete request.");
        componentDescriptorDao.deleteByClazz(tenantId, clazz);
    }

    @Override
    public boolean validate(TenantId tenantId, @NotNull ComponentDescriptor component, @NotNull JsonNode configuration) {
        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
        try {
            if (!component.getConfigurationDescriptor().has("schema")) {
                throw new DataValidationException("Configuration descriptor doesn't contain schema property!");
            }
            JsonNode configurationSchema = component.getConfigurationDescriptor().get("schema");
            ProcessingReport report = validator.validate(configurationSchema, configuration);
            return report.isSuccess();
        } catch (ProcessingException e) {
            throw new IncorrectParameterException(e.getMessage(), e);
        }
    }
}
