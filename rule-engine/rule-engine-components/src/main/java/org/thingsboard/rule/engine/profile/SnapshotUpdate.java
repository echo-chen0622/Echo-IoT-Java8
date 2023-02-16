package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import org.echoiot.server.common.data.device.profile.AlarmConditionFilterKey;
import org.echoiot.server.common.data.device.profile.AlarmConditionKeyType;

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
