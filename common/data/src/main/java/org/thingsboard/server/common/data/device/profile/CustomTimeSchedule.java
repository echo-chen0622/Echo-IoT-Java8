package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.query.DynamicValue;

import java.util.List;

@Data
public class CustomTimeSchedule implements AlarmSchedule {

    private String timezone;
    private List<CustomTimeScheduleItem> items;

    private DynamicValue<String> dynamicValue;

    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.CUSTOM;
    }

}
