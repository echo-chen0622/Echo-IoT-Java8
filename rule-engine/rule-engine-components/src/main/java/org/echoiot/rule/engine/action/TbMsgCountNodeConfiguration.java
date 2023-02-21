package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgCountNodeConfiguration implements NodeConfiguration<TbMsgCountNodeConfiguration> {

    private String telemetryPrefix;
    private int interval;

    @NotNull
    @Override
    public TbMsgCountNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgCountNodeConfiguration configuration = new TbMsgCountNodeConfiguration();
        configuration.setInterval(1);
        configuration.setTelemetryPrefix("messageCount");
        return configuration;
    }
}
