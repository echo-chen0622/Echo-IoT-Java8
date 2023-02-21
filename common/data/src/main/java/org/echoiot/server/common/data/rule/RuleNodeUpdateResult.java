package org.echoiot.server.common.data.rule;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Created by igor on 3/13/18.
 */
@Data
public class RuleNodeUpdateResult {

    @NotNull
    private final RuleNode oldRuleNode;
    @NotNull
    private final RuleNode newRuleNode;

}
