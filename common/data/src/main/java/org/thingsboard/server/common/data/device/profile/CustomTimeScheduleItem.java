package org.thingsboard.server.common.data.device.profile;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomTimeScheduleItem implements Serializable {

    private boolean enabled;
    private int dayOfWeek;
    private long startsOn;
    private long endsOn;

}
