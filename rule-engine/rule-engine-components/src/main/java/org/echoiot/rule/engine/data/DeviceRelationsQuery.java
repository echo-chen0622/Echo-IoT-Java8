package org.echoiot.rule.engine.data;

import lombok.Data;
import org.echoiot.server.common.data.relation.EntitySearchDirection;

import java.util.List;

@Data
public class DeviceRelationsQuery {
    private EntitySearchDirection direction;
    private int maxLevel = 1;
    private String relationType;
    private List<String> deviceTypes;
    private boolean fetchLastLevelOnly;
}
