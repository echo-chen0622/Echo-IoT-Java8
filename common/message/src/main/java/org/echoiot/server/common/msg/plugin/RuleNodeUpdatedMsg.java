package org.echoiot.server.common.msg.plugin;

import lombok.ToString;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.msg.MsgType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
@ToString
public class RuleNodeUpdatedMsg extends ComponentLifecycleMsg {

    public RuleNodeUpdatedMsg(TenantId tenantId, EntityId entityId) {
        super(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_NODE_UPDATED_MSG;
    }
}
