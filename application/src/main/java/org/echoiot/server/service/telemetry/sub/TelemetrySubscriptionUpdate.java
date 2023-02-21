package org.echoiot.server.service.telemetry.sub;

import org.echoiot.server.common.data.kv.TsKvEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TelemetrySubscriptionUpdate {

    private final int subscriptionId;
    private int errorCode;
    private String errorMsg;
    private Map<String, List<Object>> data;

    public TelemetrySubscriptionUpdate(int subscriptionId, @Nullable List<TsKvEntry> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = new TreeMap<>();
        if (data != null) {
            for (@NotNull TsKvEntry tsEntry : data) {
                @NotNull List<Object> values = this.data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>());
                @NotNull Object[] value = new Object[2];
                value[0] = tsEntry.getTs();
                value[1] = tsEntry.getValueAsString();
                values.add(value);
            }
        }
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, Map<String, List<Object>> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = data;
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, @NotNull SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, @NotNull SubscriptionErrorCode errorCode, @Nullable String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public Map<String, List<Object>> getData() {
        return data;
    }

    @NotNull
    public Map<String, Long> getLatestValues() {
        if (data == null) {
            return Collections.emptyMap();
        } else {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                List<Object> data = e.getValue();
                Object[] latest = (Object[]) data.get(data.size() - 1);
                return (long) latest[0];
            }));
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    @NotNull
    @Override
    public String toString() {
        return "TsSubscriptionUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data="
                + data + "]";
    }
}
