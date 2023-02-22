package org.echoiot.server.service.telemetry.sub;

import lombok.Getter;
import org.echoiot.server.common.data.alarm.Alarm;
import org.jetbrains.annotations.Nullable;

public class AlarmSubscriptionUpdate {

    @Getter
    private final int subscriptionId;
    @Getter
    private int errorCode;
    @Getter
    private String errorMsg;
    @Getter
    private Alarm alarm;
    @Getter
    private boolean alarmDeleted;

    public AlarmSubscriptionUpdate(int subscriptionId, Alarm alarm) {
        this(subscriptionId, alarm, false);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, Alarm alarm, boolean alarmDeleted) {
        super();
        this.subscriptionId = subscriptionId;
        this.alarm = alarm;
        this.alarmDeleted = alarmDeleted;
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, @Nullable String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    @Override
    public String toString() {
        return "AlarmUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", alarm="
                + alarm + "]";
    }
}
