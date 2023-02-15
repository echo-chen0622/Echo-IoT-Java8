package org.thingsboard.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.DataConstants;

@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToEdgeNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    @Override
    public TbMsgPushToEdgeNodeConfiguration defaultConfiguration() {
        TbMsgPushToEdgeNodeConfiguration configuration = new TbMsgPushToEdgeNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}
