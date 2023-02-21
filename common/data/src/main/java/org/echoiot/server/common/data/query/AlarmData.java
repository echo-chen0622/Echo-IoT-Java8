package org.echoiot.server.common.data.query;

import lombok.Getter;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmInfo;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AlarmData extends AlarmInfo {

    @Getter
    private final EntityId entityId;
    @NotNull
    @Getter
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    public AlarmData(@NotNull Alarm alarm, String originatorName, EntityId entityId) {
        super(alarm, originatorName);
        this.entityId = entityId;
        this.latest = new HashMap<>();
    }
}
