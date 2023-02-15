package org.thingsboard.server.service.stats;

import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;

public interface RuleEngineStatisticsService {

    void reportQueueStats(long ts, TbRuleEngineConsumerStats stats);
}
