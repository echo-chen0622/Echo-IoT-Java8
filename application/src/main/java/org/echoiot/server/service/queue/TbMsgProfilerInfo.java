package org.echoiot.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TbMsgProfilerInfo {
    private final UUID msgId;
    private final AtomicLong totalProcessingTime = new AtomicLong();
    private final Lock stateLock = new ReentrantLock();
    @Nullable
    private RuleNodeId currentRuleNodeId;
    private long stateChangeTime;

    public TbMsgProfilerInfo(UUID msgId) {
        this.msgId = msgId;
    }

    public void onStart(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            currentRuleNodeId = ruleNodeId;
            stateChangeTime = currentTime;
        } finally {
            stateLock.unlock();
        }
    }

    public long onEnd(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (ruleNodeId.equals(currentRuleNodeId)) {
                long processingTime = currentTime - stateChangeTime;
                stateChangeTime = currentTime;
                totalProcessingTime.addAndGet(processingTime);
                currentRuleNodeId = null;
                return processingTime;
            } else {
                log.trace("[{}] Invalid sequence of rule node processing detected. Expected [{}] but was [{}]", msgId, currentRuleNodeId, ruleNodeId);
                return 0;
            }
        } finally {
            stateLock.unlock();
        }
    }

    @Nullable
    public Map.Entry<UUID, Long> onTimeout() {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (currentRuleNodeId != null && stateChangeTime > 0) {
                long timeoutTime = currentTime - stateChangeTime;
                totalProcessingTime.addAndGet(timeoutTime);
                return new AbstractMap.SimpleEntry<>(currentRuleNodeId.getId(), timeoutTime);
            }
        } finally {
            stateLock.unlock();
        }
        return null;
    }
}
