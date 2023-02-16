package org.echoiot.server.actors.ruleChain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.actors.TbActorRef;

/**
 * Created by Echo on 19.03.18.
 */
@Data
@AllArgsConstructor
final class RuleNodeCtx {
    private final TenantId tenantId;
    private final TbActorRef chainActor;
    private final TbActorRef selfActor;
    private RuleNode self;
}
