package org.thingsboard.server.common.msg.plugin;

import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@ToString
public class RuleNodeUpdatedMsg extends ComponentLifecycleMsg {

    public RuleNodeUpdatedMsg(TenantId tenantId, EntityId entityId) {
        super(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_NODE_UPDATED_MSG;
    }
}
