package org.echoiot.server.service.queue.processing;

public interface TbRuleEngineProcessingStrategy {

    boolean isSkipTimeoutMsgs();

    TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result);

}
