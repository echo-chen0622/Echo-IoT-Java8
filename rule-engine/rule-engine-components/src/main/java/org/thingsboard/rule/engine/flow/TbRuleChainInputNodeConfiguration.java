package org.thingsboard.rule.engine.flow;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.RuleChainId;

@Data
public class TbRuleChainInputNodeConfiguration implements NodeConfiguration<TbRuleChainInputNodeConfiguration> {

    private String ruleChainId;

    @Override
    public TbRuleChainInputNodeConfiguration defaultConfiguration() {
        return new TbRuleChainInputNodeConfiguration();
    }

}
