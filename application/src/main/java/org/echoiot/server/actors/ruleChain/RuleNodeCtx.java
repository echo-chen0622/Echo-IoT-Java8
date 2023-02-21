package org.echoiot.server.actors.ruleChain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.actors.TbActorRef;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.03.18.
 */
@Data
@AllArgsConstructor
final class RuleNodeCtx {
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final TbActorRef chainActor;
    @NotNull
    private final TbActorRef selfActor;
    private RuleNode self;
}
