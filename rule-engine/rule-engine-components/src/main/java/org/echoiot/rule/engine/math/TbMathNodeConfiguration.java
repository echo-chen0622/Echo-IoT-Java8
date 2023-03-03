package org.echoiot.rule.engine.math;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.Arrays;
import java.util.List;

@Data
public class TbMathNodeConfiguration implements NodeConfiguration {

    private TbRuleNodeMathFunctionType operation;
    private List<TbMathArgument> arguments;
    private String customFunction;
    private TbMathResult result;

    @Override
    public TbMathNodeConfiguration defaultConfiguration() {
        TbMathNodeConfiguration configuration = new TbMathNodeConfiguration();
        configuration.setOperation(TbRuleNodeMathFunctionType.ADD);
        configuration.setArguments(Arrays.asList(new TbMathArgument("x", TbMathArgumentType.CONSTANT, "2"), new TbMathArgument("y", TbMathArgumentType.CONSTANT, "2")));
        configuration.setResult(new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null));
        return configuration;
    }
}
