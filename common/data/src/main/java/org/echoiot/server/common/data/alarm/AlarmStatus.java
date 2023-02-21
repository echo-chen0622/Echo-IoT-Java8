package org.echoiot.server.common.data.alarm;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 11.05.17.
 */
public enum AlarmStatus {

    ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK;

    public boolean isAck() {
        return this == ACTIVE_ACK || this == CLEARED_ACK;
    }

    public boolean isCleared() {
        return this == CLEARED_ACK || this == CLEARED_UNACK;
    }

    @NotNull
    public AlarmSearchStatus getClearSearchStatus() {
        return this.isCleared() ? AlarmSearchStatus.CLEARED : AlarmSearchStatus.ACTIVE;
    }

    @NotNull
    public AlarmSearchStatus getAckSearchStatus() {
        return this.isAck() ? AlarmSearchStatus.ACK : AlarmSearchStatus.UNACK;
    }


}
