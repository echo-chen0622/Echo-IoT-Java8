package org.echoiot.rule.engine.edge;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.DataConstants;

@Data
public class BaseTbMsgPushNodeConfiguration implements NodeConfiguration<BaseTbMsgPushNodeConfiguration> {

    private String scope;

    @Override
    public BaseTbMsgPushNodeConfiguration defaultConfiguration() {
        BaseTbMsgPushNodeConfiguration configuration = new BaseTbMsgPushNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}
