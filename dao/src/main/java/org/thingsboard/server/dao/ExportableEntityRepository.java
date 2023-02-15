package org.thingsboard.server.dao;

import java.util.UUID;

public interface ExportableEntityRepository<D> {

    D findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

}
