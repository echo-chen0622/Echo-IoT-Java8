package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.audit.AuditLog;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AuditLogDataValidator extends DataValidator<AuditLog> {

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull AuditLog auditLog) {
        if (auditLog.getEntityId() == null) {
            throw new DataValidationException("Entity Id should be specified!");
        }
        if (auditLog.getTenantId() == null) {
            throw new DataValidationException("Tenant Id should be specified!");
        }
        if (auditLog.getUserId() == null) {
            throw new DataValidationException("User Id should be specified!");
        }
    }
}
