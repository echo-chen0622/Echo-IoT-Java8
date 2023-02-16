package org.echoiot.server.common.data;

import org.echoiot.server.common.data.id.RuleChainId;

public interface HasRuleEngineProfile {

    RuleChainId getDefaultRuleChainId();

    String getDefaultQueueName();

}
