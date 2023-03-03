package org.echoiot.rule.engine.telemetry;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.DataConstants;

import java.util.Collections;
import java.util.List;

@Data
public class TbMsgDeleteAttributesNodeConfiguration implements NodeConfiguration {

    private String scope;
    private List<String> keys;
    private boolean sendAttributesDeletedNotification;
    private boolean notifyDevice;

    @Override
    public TbMsgDeleteAttributesNodeConfiguration defaultConfiguration() {
        TbMsgDeleteAttributesNodeConfiguration configuration = new TbMsgDeleteAttributesNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setKeys(Collections.emptyList());
        configuration.setSendAttributesDeletedNotification(false);
        configuration.setNotifyDevice(false);
        return configuration;
    }
}
