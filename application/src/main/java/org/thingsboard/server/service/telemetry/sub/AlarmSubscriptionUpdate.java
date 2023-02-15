package org.thingsboard.server.service.telemetry.sub;

import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.AlarmData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AlarmSubscriptionUpdate {

    @Getter
    private int subscriptionId;
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

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
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
