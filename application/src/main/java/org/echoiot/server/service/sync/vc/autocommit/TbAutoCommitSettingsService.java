package org.echoiot.server.service.sync.vc.autocommit;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.AutoCommitSettings;

public interface TbAutoCommitSettingsService {

    AutoCommitSettings get(TenantId tenantId);

    AutoCommitSettings save(TenantId tenantId, AutoCommitSettings settings);

    boolean delete(TenantId tenantId);

}
