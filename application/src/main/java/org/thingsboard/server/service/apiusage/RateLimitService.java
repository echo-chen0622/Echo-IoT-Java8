package org.thingsboard.server.service.apiusage;

import org.thingsboard.server.common.data.id.TenantId;

public interface RateLimitService {

    boolean checkEntityExportLimit(TenantId tenantId);

    boolean checkEntityImportLimit(TenantId tenantId);

}
