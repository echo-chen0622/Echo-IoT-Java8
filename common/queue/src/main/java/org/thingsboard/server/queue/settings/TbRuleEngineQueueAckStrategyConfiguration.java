package org.thingsboard.server.queue.settings;

import lombok.Data;

@Data
@Deprecated
public class TbRuleEngineQueueAckStrategyConfiguration {

    private String type;
    private int retries;
    private double failurePercentage;
    private long pauseBetweenRetries;
    private long maxPauseBetweenRetries;

}
