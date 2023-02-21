package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ApiUsageDataValidator extends DataValidator<ApiUsageState> {

    @Lazy
    @Resource
    private TenantService tenantService;

    @Override
    protected void validateDataImpl(@NotNull TenantId requestTenantId, @NotNull ApiUsageState apiUsageState) {
        if (apiUsageState.getTenantId() == null) {
            throw new DataValidationException("ApiUsageState should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(apiUsageState.getTenantId()) && !requestTenantId.equals(TenantId.SYS_TENANT_ID)) {
                throw new DataValidationException("ApiUsageState is referencing to non-existent tenant!");
            }
        }
        if (apiUsageState.getEntityId() == null) {
            throw new DataValidationException("UsageRecord should be assigned to entity!");
        } else if (apiUsageState.getEntityId().getEntityType() != EntityType.TENANT && apiUsageState.getEntityId().getEntityType() != EntityType.CUSTOMER) {
            throw new DataValidationException("Only Tenant and Customer Usage Records are supported!");
        }
    }
}
