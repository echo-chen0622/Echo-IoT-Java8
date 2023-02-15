package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Set;

class SnapshotUpdate {

    @Getter
    private final AlarmConditionKeyType type;
    @Getter
    private final Set<AlarmConditionFilterKey> keys;

    SnapshotUpdate(AlarmConditionKeyType type, Set<AlarmConditionFilterKey> keys) {
        this.type = type;
        this.keys = keys;
    }

    boolean hasUpdate(){
        return !keys.isEmpty();
    }
}
