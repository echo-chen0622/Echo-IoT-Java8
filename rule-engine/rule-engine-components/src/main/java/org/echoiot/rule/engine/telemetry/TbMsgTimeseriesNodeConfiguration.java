package org.echoiot.rule.engine.telemetry;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgTimeseriesNodeConfiguration implements NodeConfiguration {

    private long defaultTTL;
    private boolean skipLatestPersistence;
    private boolean useServerTs;

    @Override
    public TbMsgTimeseriesNodeConfiguration defaultConfiguration() {
        TbMsgTimeseriesNodeConfiguration configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setDefaultTTL(0L);
        configuration.setSkipLatestPersistence(false);
        configuration.setUseServerTs(false);
        return configuration;
    }
}
