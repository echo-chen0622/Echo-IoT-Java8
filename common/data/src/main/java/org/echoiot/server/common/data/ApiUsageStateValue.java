package org.echoiot.server.common.data;

import org.jetbrains.annotations.NotNull;

public enum ApiUsageStateValue {

    ENABLED, WARNING, DISABLED;


    @NotNull
    public static ApiUsageStateValue toMoreRestricted(@NotNull ApiUsageStateValue a, @NotNull ApiUsageStateValue b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }
}
