package org.echoiot.server.common.msg.queue;

import lombok.Getter;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.jetbrains.annotations.NotNull;

public class RuleNodeInfo {
    @NotNull
    private final String label;
    @Getter
    private final RuleNodeId ruleNodeId;

    public RuleNodeInfo(RuleNodeId id, String ruleChainName, String ruleNodeName) {
        this.ruleNodeId = id;
        this.label = "[RuleChain: " + ruleChainName + "|RuleNode: " + ruleNodeName + "(" + id + ")]";
    }

    @NotNull
    @Override
    public String toString() {
        return label;
    }
}
