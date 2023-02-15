package org.thingsboard.server.service.sync.vc.autocommit;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.AutoCommitSettings;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

public interface TbAutoCommitSettingsService {

    AutoCommitSettings get(TenantId tenantId);

    AutoCommitSettings save(TenantId tenantId, AutoCommitSettings settings);

    boolean delete(TenantId tenantId);

}
