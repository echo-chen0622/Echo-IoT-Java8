package org.echoiot.rule.engine.flow;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbRuleChainInputNodeConfiguration implements NodeConfiguration<TbRuleChainInputNodeConfiguration> {

    private String ruleChainId;

    @NotNull
    @Override
    public TbRuleChainInputNodeConfiguration defaultConfiguration() {
        return new TbRuleChainInputNodeConfiguration();
    }

}
