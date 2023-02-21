package org.echoiot.server.transport.lwm2m.server.uplink;

import org.jetbrains.annotations.NotNull;

public enum LwM2mTypeServer {
    BOOTSTRAP(0, "bootstrap"),
    CLIENT(1, "client");

    public int code;
    public String type;

    LwM2mTypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    @NotNull
    public static LwM2mTypeServer fromLwM2mTypeServer(String type) {
        for (@NotNull LwM2mTypeServer sm : LwM2mTypeServer.values()) {
            if (sm.type.equals(type)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeServer type : %d", type));
    }
}
