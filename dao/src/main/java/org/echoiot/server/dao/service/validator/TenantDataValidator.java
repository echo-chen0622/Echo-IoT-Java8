package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TenantDataValidator extends DataValidator<Tenant> {

    @Autowired
    private TenantDao tenantDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, Tenant tenant) {
        if (StringUtils.isEmpty(tenant.getTitle())) {
            throw new DataValidationException("Tenant title should be specified!");
        }
        if (!StringUtils.isEmpty(tenant.getEmail())) {
            validateEmail(tenant.getEmail());
        }
    }

    @Override
    protected Tenant validateUpdate(TenantId tenantId, Tenant tenant) {
        Tenant old = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant!");
        }
        return old;
    }

}