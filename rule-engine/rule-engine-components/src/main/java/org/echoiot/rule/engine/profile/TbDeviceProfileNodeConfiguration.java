package org.echoiot.rule.engine.profile;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbDeviceProfileNodeConfiguration implements NodeConfiguration<TbDeviceProfileNodeConfiguration> {

    private boolean persistAlarmRulesState;
    private boolean fetchAlarmRulesStateOnStart;

    @NotNull
    @Override
    public TbDeviceProfileNodeConfiguration defaultConfiguration() {
        return new TbDeviceProfileNodeConfiguration();
    }
}
