package org.echoiot.server.common.data.rule;

import lombok.Data;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.RuleNodeStateId;

@Data
public class RuleNodeState extends BaseData<RuleNodeStateId> {

    private RuleNodeId ruleNodeId;
    private EntityId entityId;
    private String stateData;

    public RuleNodeState() {
        super();
    }

    public RuleNodeState(RuleNodeStateId id) {
        super(id);
    }

    public RuleNodeState(RuleNodeState event) {
        super(event);
    }
}
