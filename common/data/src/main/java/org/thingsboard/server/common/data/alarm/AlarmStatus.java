package org.thingsboard.server.common.data.alarm;

/**
 * Created by ashvayka on 11.05.17.
 */
public enum AlarmStatus {

    ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK;

    public boolean isAck() {
        return this == ACTIVE_ACK || this == CLEARED_ACK;
    }

    public boolean isCleared() {
        return this == CLEARED_ACK || this == CLEARED_UNACK;
    }

    public AlarmSearchStatus getClearSearchStatus() {
        return this.isCleared() ? AlarmSearchStatus.CLEARED : AlarmSearchStatus.ACTIVE;
    }

    public AlarmSearchStatus getAckSearchStatus() {
        return this.isAck() ? AlarmSearchStatus.ACK : AlarmSearchStatus.UNACK;
    }


}
