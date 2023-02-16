package org.echoiot.rule.engine.data;

import lombok.Data;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

@Data
public class RelationsQuery {

    private EntitySearchDirection direction;
    private int maxLevel = 1;
    private List<RelationEntityTypeFilter> filters;
    private boolean fetchLastLevelOnly = false;
}
