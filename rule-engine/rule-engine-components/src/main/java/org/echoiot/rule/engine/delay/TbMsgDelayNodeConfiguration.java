package org.echoiot.rule.engine.delay;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgDelayNodeConfiguration implements NodeConfiguration<TbMsgDelayNodeConfiguration> {

    private int periodInSeconds;
    private int maxPendingMsgs;
    private String periodInSecondsPattern;
    private boolean useMetadataPeriodInSecondsPatterns;

    @Override
    public TbMsgDelayNodeConfiguration defaultConfiguration() {
        TbMsgDelayNodeConfiguration configuration = new TbMsgDelayNodeConfiguration();
        configuration.setPeriodInSeconds(60);
        configuration.setMaxPendingMsgs(1000);
        configuration.setUseMetadataPeriodInSecondsPatterns(false);
        return configuration;
    }
}
