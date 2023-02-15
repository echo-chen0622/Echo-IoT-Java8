package org.thingsboard.server.queue.settings;

import lombok.Data;

@Data
@Deprecated
public class TbRuleEngineQueueSubmitStrategyConfiguration {

    private String type;
    private int batchSize;

}
