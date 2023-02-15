package org.thingsboard.server.common.msg.plugin;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@ToString
public class ComponentLifecycleMsg implements TenantAwareMsg, ToAllNodesMsg {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final ComponentLifecycleEvent event;

    public ComponentLifecycleMsg(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent event) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.event = event;
    }

    public Optional<RuleChainId> getRuleChainId() {
        return entityId.getEntityType() == EntityType.RULE_CHAIN ? Optional.of((RuleChainId) entityId) : Optional.empty();
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.COMPONENT_LIFE_CYCLE_MSG;
    }
}
