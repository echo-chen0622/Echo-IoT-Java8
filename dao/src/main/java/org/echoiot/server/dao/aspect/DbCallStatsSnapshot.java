package org.echoiot.server.dao.aspect;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@Builder
public class DbCallStatsSnapshot {

    @NotNull
    private final TenantId tenantId;
    private final int totalSuccess;
    private final int totalFailure;
    private final long totalTiming;
    @NotNull
    private final Map<String, MethodCallStatsSnapshot> methodStats;

    public int getTotalCalls() {
        return totalSuccess + totalFailure;
    }

}
