package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.query.DynamicValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class CustomTimeSchedule implements AlarmSchedule {

    private String timezone;
    private List<CustomTimeScheduleItem> items;

    private DynamicValue<String> dynamicValue;

    @NotNull
    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.CUSTOM;
    }

}
