package org.thingsboard.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.DataConstants;

@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToCloudNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    @Override
    public TbMsgPushToCloudNodeConfiguration defaultConfiguration() {
        TbMsgPushToCloudNodeConfiguration configuration = new TbMsgPushToCloudNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}
