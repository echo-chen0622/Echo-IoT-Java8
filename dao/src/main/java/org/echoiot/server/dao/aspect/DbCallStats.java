package org.echoiot.server.dao.aspect;

import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Data
public class DbCallStats {

    @NotNull
    private final TenantId tenantId;
    private final ConcurrentMap<String, MethodCallStats> methodStats = new ConcurrentHashMap<>();
    private final AtomicInteger successCalls = new AtomicInteger();
    private final AtomicInteger failureCalls = new AtomicInteger();

    public void onMethodCall(String methodName, boolean success, long executionTime) {
        @NotNull var methodCallStats = methodStats.computeIfAbsent(methodName, m -> new MethodCallStats());
        methodCallStats.getExecutions().incrementAndGet();
        methodCallStats.getTiming().addAndGet(executionTime);
        if (success) {
            successCalls.incrementAndGet();
        } else {
            failureCalls.incrementAndGet();
            methodCallStats.getFailures().incrementAndGet();
        }
    }

    public DbCallStatsSnapshot snapshot() {
        return DbCallStatsSnapshot.builder()
                .tenantId(tenantId)
                .totalSuccess(successCalls.get())
                .totalFailure(failureCalls.get())
                .methodStats(methodStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot())))
                .totalTiming(methodStats.values().stream().map(MethodCallStats::getTiming).map(AtomicLong::get).reduce(0L, Long::sum))
                .build();
    }

}
