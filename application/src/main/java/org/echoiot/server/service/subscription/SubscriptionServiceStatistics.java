package org.echoiot.server.service.subscription;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class SubscriptionServiceStatistics {
    @NotNull
    private AtomicInteger alarmQueryInvocationCnt = new AtomicInteger();
    @NotNull
    private AtomicInteger regularQueryInvocationCnt = new AtomicInteger();
    @NotNull
    private AtomicInteger dynamicQueryInvocationCnt = new AtomicInteger();
    @NotNull
    private AtomicLong alarmQueryTimeSpent = new AtomicLong();
    @NotNull
    private AtomicLong regularQueryTimeSpent = new AtomicLong();
    @NotNull
    private AtomicLong dynamicQueryTimeSpent = new AtomicLong();
}
