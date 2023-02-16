package org.echoiot.server.service.entitiy.tenant.profile;

import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.TenantId;

public interface TbTenantProfileService {
    TenantProfile save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile) throws ThingsboardException;

    void delete(TenantId tenantId, TenantProfile tenantProfile) throws ThingsboardException;
}
