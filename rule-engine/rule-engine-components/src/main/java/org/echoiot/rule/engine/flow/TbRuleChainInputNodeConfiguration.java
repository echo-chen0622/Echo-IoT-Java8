package org.echoiot.rule.engine.flow;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbRuleChainInputNodeConfiguration implements NodeConfiguration {

    private String ruleChainId;

    @Override
    public TbRuleChainInputNodeConfiguration defaultConfiguration() {
        return new TbRuleChainInputNodeConfiguration();
    }

}
