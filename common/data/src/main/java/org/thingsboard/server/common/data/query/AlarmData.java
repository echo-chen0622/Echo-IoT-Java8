package org.thingsboard.server.common.data.query;

import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlarmData extends AlarmInfo {

    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public AlarmData(Alarm alarm, String originatorName, EntityId entityId) {
        super(alarm, originatorName);
        this.entityId = entityId;
        this.latest = new HashMap<>();
    }
}
