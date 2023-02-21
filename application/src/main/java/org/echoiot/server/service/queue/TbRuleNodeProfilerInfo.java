package org.echoiot.server.service.queue;

import lombok.Getter;
import org.echoiot.server.common.msg.queue.RuleNodeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TbRuleNodeProfilerInfo {
    @Getter
    private final UUID ruleNodeId;
    @NotNull
    @Getter
    private final String label;
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicLong executionTime = new AtomicLong(0);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);

    public TbRuleNodeProfilerInfo(@NotNull RuleNodeInfo ruleNodeInfo) {
        this.ruleNodeId = ruleNodeInfo.getRuleNodeId().getId();
        this.label = ruleNodeInfo.toString();
    }

    public TbRuleNodeProfilerInfo(UUID ruleNodeId) {
        this.ruleNodeId = ruleNodeId;
        this.label = "";
    }

    public void record(long processingTime) {
        executionCount.incrementAndGet();
        executionTime.addAndGet(processingTime);
        while (true) {
            long value = maxExecutionTime.get();
            if (value >= processingTime) {
                break;
            }
            if (maxExecutionTime.compareAndSet(value, processingTime)) {
                break;
            }
        }
    }

    int getExecutionCount() {
        return executionCount.get();
    }

    long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }

    double getAvgExecutionTime() {
        double executionCnt = executionCount.get();
        if (executionCnt > 0) {
            return executionTime.get() / executionCnt;
        } else {
            return 0.0;
        }
    }

}
