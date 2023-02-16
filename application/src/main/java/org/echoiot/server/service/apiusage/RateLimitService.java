package org.echoiot.server.service.apiusage;

import org.echoiot.server.common.data.id.TenantId;

public interface RateLimitService {

    boolean checkEntityExportLimit(TenantId tenantId);

    boolean checkEntityImportLimit(TenantId tenantId);

}
