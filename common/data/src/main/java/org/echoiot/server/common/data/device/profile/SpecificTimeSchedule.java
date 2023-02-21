package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.query.DynamicValue;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Data
public class SpecificTimeSchedule implements AlarmSchedule {

    private String timezone;
    private Set<Integer> daysOfWeek;
    private long startsOn;
    private long endsOn;

    private DynamicValue<String> dynamicValue;

    @NotNull
    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.SPECIFIC_TIME;
    }

}
