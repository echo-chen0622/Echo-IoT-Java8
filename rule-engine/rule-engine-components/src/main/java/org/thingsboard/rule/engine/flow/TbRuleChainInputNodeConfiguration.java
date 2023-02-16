package org.thingsboard.rule.engine.flow;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbRuleChainInputNodeConfiguration implements NodeConfiguration<TbRuleChainInputNodeConfiguration> {

    private String ruleChainId;

    @Override
    public TbRuleChainInputNodeConfiguration defaultConfiguration() {
        return new TbRuleChainInputNodeConfiguration();
    }

}
