package org.echoiot.server.dao.tenant;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;

@Data
public class TenantEvictEvent {
    private final TenantId tenantId;
    private final boolean invalidateExists;
}
