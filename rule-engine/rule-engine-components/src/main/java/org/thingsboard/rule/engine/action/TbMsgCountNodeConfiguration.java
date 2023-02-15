package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgCountNodeConfiguration implements NodeConfiguration<TbMsgCountNodeConfiguration> {

    private String telemetryPrefix;
    private int interval;

    @Override
    public TbMsgCountNodeConfiguration defaultConfiguration() {
        TbMsgCountNodeConfiguration configuration = new TbMsgCountNodeConfiguration();
        configuration.setInterval(1);
        configuration.setTelemetryPrefix("messageCount");
        return configuration;
    }
}
