package org.thingsboard.server.common.data.queue;

import lombok.Data;

@Data
public class ProcessingStrategy {
    private ProcessingStrategyType type;
    private int retries;
    private double failurePercentage;
    private long pauseBetweenRetries;
    private long maxPauseBetweenRetries;
}
