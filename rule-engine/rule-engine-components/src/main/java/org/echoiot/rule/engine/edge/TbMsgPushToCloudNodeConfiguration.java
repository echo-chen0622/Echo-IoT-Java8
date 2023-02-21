package org.echoiot.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.DataConstants;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToCloudNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    @NotNull
    @Override
    public TbMsgPushToCloudNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgPushToCloudNodeConfiguration configuration = new TbMsgPushToCloudNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}
