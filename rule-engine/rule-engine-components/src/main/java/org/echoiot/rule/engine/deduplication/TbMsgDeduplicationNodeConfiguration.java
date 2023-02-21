package org.echoiot.rule.engine.deduplication;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgDeduplicationNodeConfiguration implements NodeConfiguration<TbMsgDeduplicationNodeConfiguration> {

    private int interval;
    private DeduplicationStrategy strategy;

    // only for DeduplicationStrategy.ALL:
    private String outMsgType;
    private String queueName;

    // Advanced settings:
    private int maxPendingMsgs;
    private int maxRetries;

    @NotNull
    @Override
    public TbMsgDeduplicationNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgDeduplicationNodeConfiguration configuration = new TbMsgDeduplicationNodeConfiguration();
        configuration.setInterval(60);
        configuration.setStrategy(DeduplicationStrategy.FIRST);
        configuration.setMaxPendingMsgs(100);
        configuration.setMaxRetries(3);
        return configuration;
    }
}
