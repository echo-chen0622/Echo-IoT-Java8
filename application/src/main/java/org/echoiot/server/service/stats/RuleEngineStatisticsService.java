package org.echoiot.server.service.stats;

import org.echoiot.server.service.queue.TbRuleEngineConsumerStats;

public interface RuleEngineStatisticsService {

    void reportQueueStats(long ts, TbRuleEngineConsumerStats stats);
}
