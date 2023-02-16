package org.thingsboard.rule.engine.flow;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbCheckpointNodeConfiguration implements NodeConfiguration<TbCheckpointNodeConfiguration> {

    private String queueName;

    @Override
    public TbCheckpointNodeConfiguration defaultConfiguration() {
        return new TbCheckpointNodeConfiguration();
    }
}
