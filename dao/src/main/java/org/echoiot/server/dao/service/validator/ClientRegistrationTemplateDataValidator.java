package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ClientRegistrationTemplateDataValidator extends DataValidator<OAuth2ClientRegistrationTemplate> {

    @Override
    protected void validateCreate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
    }

    @Nullable
    @Override
    protected OAuth2ClientRegistrationTemplate validateUpdate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return null;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (StringUtils.isEmpty(clientRegistrationTemplate.getProviderId())) {
            throw new DataValidationException("Provider ID should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig() == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getType() == null) {
            throw new DataValidationException("Mapper type should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getBasic() == null) {
            throw new DataValidationException("Basic mapper config should be specified!");
        }
    }
}
