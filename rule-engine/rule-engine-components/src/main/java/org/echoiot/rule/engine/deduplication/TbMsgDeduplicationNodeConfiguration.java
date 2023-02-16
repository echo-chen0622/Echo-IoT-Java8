package org.echoiot.rule.engine.deduplication;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

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

    @Override
    public TbMsgDeduplicationNodeConfiguration defaultConfiguration() {
        TbMsgDeduplicationNodeConfiguration configuration = new TbMsgDeduplicationNodeConfiguration();
        configuration.setInterval(60);
        configuration.setStrategy(DeduplicationStrategy.FIRST);
        configuration.setMaxPendingMsgs(100);
        configuration.setMaxRetries(3);
        return configuration;
    }
}
