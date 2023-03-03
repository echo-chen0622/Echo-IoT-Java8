package org.echoiot.rule.engine.telemetry;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.DataConstants;

@Data
public class TbMsgAttributesNodeConfiguration implements NodeConfiguration {

    private String scope;

    private Boolean notifyDevice;
    private boolean sendAttributesUpdatedNotification;

    @Override
    public TbMsgAttributesNodeConfiguration defaultConfiguration() {
        TbMsgAttributesNodeConfiguration configuration = new TbMsgAttributesNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setNotifyDevice(false);
        configuration.setSendAttributesUpdatedNotification(false);
        return configuration;
    }
}
