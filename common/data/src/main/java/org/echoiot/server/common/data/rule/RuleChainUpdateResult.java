package org.echoiot.server.common.data.rule;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleChainUpdateResult {

    private final boolean success;
    @NotNull
    private final List<RuleNodeUpdateResult> updatedRuleNodes;

    @NotNull
    public static RuleChainUpdateResult failed(){
        return new RuleChainUpdateResult(false, null);
    }

    @NotNull
    public static RuleChainUpdateResult successful(List<RuleNodeUpdateResult> updatedRuleNodes){
        return new RuleChainUpdateResult(true, updatedRuleNodes);
    }

}
