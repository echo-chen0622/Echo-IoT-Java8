package org.echoiot.server.dao.tenant;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantProfileId;

@Data
public class TenantProfileEvictEvent {
    private final TenantProfileId tenantProfileId;
    private final boolean defaultProfile;
}
