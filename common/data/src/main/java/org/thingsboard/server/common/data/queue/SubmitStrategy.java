package org.thingsboard.server.common.data.queue;

import lombok.Data;

@Data
public class SubmitStrategy {
    private SubmitStrategyType type;
    private int batchSize;
}
