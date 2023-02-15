package org.thingsboard.rule.engine.flow;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.QueueId;

@Data
public class TbCheckpointNodeConfiguration implements NodeConfiguration<TbCheckpointNodeConfiguration> {

    private String queueName;

    @Override
    public TbCheckpointNodeConfiguration defaultConfiguration() {
        return new TbCheckpointNodeConfiguration();
    }
}
