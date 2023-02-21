package org.echoiot.rule.engine.telemetry;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.DataConstants;
import org.jetbrains.annotations.NotNull;

@Data
public class TbMsgAttributesNodeConfiguration implements NodeConfiguration<TbMsgAttributesNodeConfiguration> {

    private String scope;

    private Boolean notifyDevice;
    private boolean sendAttributesUpdatedNotification;

    @NotNull
    @Override
    public TbMsgAttributesNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgAttributesNodeConfiguration configuration = new TbMsgAttributesNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setNotifyDevice(false);
        configuration.setSendAttributesUpdatedNotification(false);
        return configuration;
    }
}
