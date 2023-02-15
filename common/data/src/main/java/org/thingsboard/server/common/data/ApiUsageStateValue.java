package org.thingsboard.server.common.data;

public enum ApiUsageStateValue {

    ENABLED, WARNING, DISABLED;


    public static ApiUsageStateValue toMoreRestricted(ApiUsageStateValue a, ApiUsageStateValue b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }
}
