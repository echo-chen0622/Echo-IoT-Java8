package org.thingsboard.server.actors.ruleChain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;

/**
 * Created by ashvayka on 19.03.18.
 */
@Data
@AllArgsConstructor
final class RuleNodeCtx {
    private final TenantId tenantId;
    private final TbActorRef chainActor;
    private final TbActorRef selfActor;
    private RuleNode self;
}
