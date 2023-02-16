package org.echoiot.rule.engine.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbFetchDeviceCredentialsNodeConfiguration implements NodeConfiguration<TbFetchDeviceCredentialsNodeConfiguration> {

    private boolean fetchToMetadata;

    @Override
    public TbFetchDeviceCredentialsNodeConfiguration defaultConfiguration() {
        TbFetchDeviceCredentialsNodeConfiguration configuration = new TbFetchDeviceCredentialsNodeConfiguration();
        configuration.setFetchToMetadata(true);
        return configuration;
    }
}
