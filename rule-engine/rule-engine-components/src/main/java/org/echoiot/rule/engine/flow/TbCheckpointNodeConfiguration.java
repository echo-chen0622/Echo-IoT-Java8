package org.echoiot.rule.engine.flow;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbCheckpointNodeConfiguration implements NodeConfiguration<TbCheckpointNodeConfiguration> {

    private String queueName;

    @NotNull
    @Override
    public TbCheckpointNodeConfiguration defaultConfiguration() {
        return new TbCheckpointNodeConfiguration();
    }
}
