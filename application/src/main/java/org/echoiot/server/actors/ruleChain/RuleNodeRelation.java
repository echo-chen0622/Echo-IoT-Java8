package org.echoiot.server.actors.ruleChain;

import lombok.Data;
import org.echoiot.server.common.data.id.EntityId;

/**
 * Created by Echo on 19.03.18.
 */

@Data
final class RuleNodeRelation {

    private final EntityId in;
    private final EntityId out;
    private final String type;

}
