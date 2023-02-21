package org.echoiot.rule.engine.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbFetchDeviceCredentialsNodeConfiguration implements NodeConfiguration<TbFetchDeviceCredentialsNodeConfiguration> {

    private boolean fetchToMetadata;

    @NotNull
    @Override
    public TbFetchDeviceCredentialsNodeConfiguration defaultConfiguration() {
        @NotNull TbFetchDeviceCredentialsNodeConfiguration configuration = new TbFetchDeviceCredentialsNodeConfiguration();
        configuration.setFetchToMetadata(true);
        return configuration;
    }
}
