package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public enum AlarmSearchStatus {

    ANY(AlarmStatus.values()),
    ACTIVE(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK),
    CLEARED(AlarmStatus.CLEARED_ACK, AlarmStatus.CLEARED_UNACK),
    ACK(AlarmStatus.ACTIVE_ACK, AlarmStatus.CLEARED_ACK),
    UNACK(AlarmStatus.ACTIVE_UNACK, AlarmStatus.CLEARED_UNACK);

    @JsonIgnore
    @Getter
    private Set<AlarmStatus> statuses;

    AlarmSearchStatus(AlarmStatus... statuses) {
        this.statuses = new LinkedHashSet<>(Arrays.asList(statuses));
    }
}
