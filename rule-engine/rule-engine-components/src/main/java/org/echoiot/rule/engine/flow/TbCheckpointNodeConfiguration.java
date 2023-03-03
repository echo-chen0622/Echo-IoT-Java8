package org.echoiot.rule.engine.flow;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbCheckpointNodeConfiguration implements NodeConfiguration {

    private String queueName;

    @Override
    public TbCheckpointNodeConfiguration defaultConfiguration() {
        return new TbCheckpointNodeConfiguration();
    }
}
