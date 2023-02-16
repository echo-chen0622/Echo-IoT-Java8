package org.echoiot.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AlarmDataValidator extends DataValidator<Alarm> {

    private final TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, Alarm alarm) {
        if (StringUtils.isEmpty(alarm.getType())) {
            throw new DataValidationException("Alarm type should be specified!");
        }
        if (alarm.getOriginator() == null) {
            throw new DataValidationException("Alarm originator should be specified!");
        }
        if (alarm.getSeverity() == null) {
            throw new DataValidationException("Alarm severity should be specified!");
        }
        if (alarm.getStatus() == null) {
            throw new DataValidationException("Alarm status should be specified!");
        }
        if (alarm.getTenantId() == null) {
            throw new DataValidationException("Alarm should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(alarm.getTenantId())) {
                throw new DataValidationException("Alarm is referencing to non-existent tenant!");
            }
        }
    }
}
