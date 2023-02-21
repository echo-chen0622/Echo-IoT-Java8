package org.echoiot.rule.engine.telemetry;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgTimeseriesNodeConfiguration implements NodeConfiguration<TbMsgTimeseriesNodeConfiguration> {

    private long defaultTTL;
    private boolean skipLatestPersistence;
    private boolean useServerTs;

    @NotNull
    @Override
    public TbMsgTimeseriesNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgTimeseriesNodeConfiguration configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setDefaultTTL(0L);
        configuration.setSkipLatestPersistence(false);
        configuration.setUseServerTs(false);
        return configuration;
    }
}
