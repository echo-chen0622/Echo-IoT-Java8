package org.thingsboard.server.queue.discovery;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class TenantRoutingInfo {
    private final TenantId tenantId;
    private final boolean isolatedTbRuleEngine;
}
