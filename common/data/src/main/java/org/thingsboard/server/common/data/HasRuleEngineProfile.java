package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.RuleChainId;

public interface HasRuleEngineProfile {

    RuleChainId getDefaultRuleChainId();

    String getDefaultQueueName();

}
