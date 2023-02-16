package org.echoiot.server.actors.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.msg.tools.TbRateLimits;

@Data
@AllArgsConstructor
public class DebugTbRateLimits {

    private TbRateLimits tbRateLimits;
    private boolean ruleChainEventSaved;

}
