package org.echoiot.server.queue.discovery;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;

@Data
public class TenantRoutingInfo {
    private final TenantId tenantId;
    private final boolean isolatedTbRuleEngine;
}
