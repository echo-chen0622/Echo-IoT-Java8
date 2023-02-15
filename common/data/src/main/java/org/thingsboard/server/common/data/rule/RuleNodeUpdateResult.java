package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.Map;

/**
 * Created by igor on 3/13/18.
 */
@Data
public class RuleNodeUpdateResult {

    private final RuleNode oldRuleNode;
    private final RuleNode newRuleNode;

}
