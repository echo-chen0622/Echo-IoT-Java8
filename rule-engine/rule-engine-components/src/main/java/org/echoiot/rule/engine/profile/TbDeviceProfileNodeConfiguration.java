package org.echoiot.rule.engine.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbDeviceProfileNodeConfiguration implements NodeConfiguration {

    private boolean persistAlarmRulesState;
    private boolean fetchAlarmRulesStateOnStart;

    @Override
    public TbDeviceProfileNodeConfiguration defaultConfiguration() {
        return new TbDeviceProfileNodeConfiguration();
    }
}
