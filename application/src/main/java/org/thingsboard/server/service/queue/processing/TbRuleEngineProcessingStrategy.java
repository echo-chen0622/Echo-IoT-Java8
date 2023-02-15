package org.thingsboard.server.service.queue.processing;

public interface TbRuleEngineProcessingStrategy {

    boolean isSkipTimeoutMsgs();

    TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result);

}
