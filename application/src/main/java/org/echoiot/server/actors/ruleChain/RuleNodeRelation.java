package org.echoiot.server.actors.ruleChain;

import lombok.Data;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 19.03.18.
 */

@Data
final class RuleNodeRelation {

    @NotNull
    private final EntityId in;
    @NotNull
    private final EntityId out;
    @NotNull
    private final String type;

}
