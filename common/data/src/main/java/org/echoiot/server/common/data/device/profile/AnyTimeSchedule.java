package org.echoiot.server.common.data.device.profile;

import org.echoiot.server.common.data.query.DynamicValue;
import org.jetbrains.annotations.Nullable;

public class AnyTimeSchedule implements AlarmSchedule {

    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.ANY_TIME;
    }

    @Nullable
    @Override
    public DynamicValue<String> getDynamicValue() {
        return null;
    }

}
