package org.echoiot.server.common.data.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Authority {

    SYS_ADMIN(0),
    TENANT_ADMIN(1),
    CUSTOMER_USER(2),
    REFRESH_TOKEN(10),
    PRE_VERIFICATION_TOKEN(11);

    private final int code;

    Authority(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static Authority parse(@Nullable String value) {
        @Nullable Authority authority = null;
        if (value != null && value.length() != 0) {
            for (@NotNull Authority current : Authority.values()) {
                if (current.name().equalsIgnoreCase(value)) {
                    authority = current;
                    break;
                }
            }
        }
        return authority;
    }
}
