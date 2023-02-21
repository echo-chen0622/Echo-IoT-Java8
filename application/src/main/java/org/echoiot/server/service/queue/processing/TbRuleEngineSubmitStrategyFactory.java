package org.echoiot.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TbRuleEngineSubmitStrategyFactory {

    @NotNull
    public TbRuleEngineSubmitStrategy newInstance(String name, @NotNull SubmitStrategy submitStrategy) {
        switch (submitStrategy.getType()) {
            case BURST:
                return new BurstTbRuleEngineSubmitStrategy(name);
            case BATCH:
                return new BatchTbRuleEngineSubmitStrategy(name, submitStrategy.getBatchSize());
            case SEQUENTIAL_BY_ORIGINATOR:
                return new SequentialByOriginatorIdTbRuleEngineSubmitStrategy(name);
            case SEQUENTIAL_BY_TENANT:
                return new SequentialByTenantIdTbRuleEngineSubmitStrategy(name);
            case SEQUENTIAL:
                return new SequentialTbRuleEngineSubmitStrategy(name);
            default:
                throw new RuntimeException("TbRuleEngineProcessingStrategy with type " + submitStrategy.getType() + " is not supported!");
        }
    }

}
