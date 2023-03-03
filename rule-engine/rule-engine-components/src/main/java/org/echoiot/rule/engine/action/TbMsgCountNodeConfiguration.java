package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgCountNodeConfiguration implements NodeConfiguration {

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
