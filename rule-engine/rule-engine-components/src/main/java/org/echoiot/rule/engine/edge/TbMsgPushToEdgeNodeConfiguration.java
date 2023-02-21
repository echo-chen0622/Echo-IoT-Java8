package org.echoiot.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.DataConstants;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToEdgeNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    @NotNull
    @Override
    public TbMsgPushToEdgeNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgPushToEdgeNodeConfiguration configuration = new TbMsgPushToEdgeNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}
