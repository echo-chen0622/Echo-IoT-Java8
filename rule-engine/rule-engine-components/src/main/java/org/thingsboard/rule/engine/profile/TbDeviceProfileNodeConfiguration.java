package org.thingsboard.rule.engine.profile;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbDeviceProfileNodeConfiguration implements NodeConfiguration<TbDeviceProfileNodeConfiguration> {

    private boolean persistAlarmRulesState;
    private boolean fetchAlarmRulesStateOnStart;

    @Override
    public TbDeviceProfileNodeConfiguration defaultConfiguration() {
        return new TbDeviceProfileNodeConfiguration();
    }
}
