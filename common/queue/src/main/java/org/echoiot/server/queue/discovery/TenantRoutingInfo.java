package org.echoiot.server.queue.discovery;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@Data
public class TenantRoutingInfo {
    @NotNull
    private final TenantId tenantId;
    private final boolean isolatedTbRuleEngine;
}
