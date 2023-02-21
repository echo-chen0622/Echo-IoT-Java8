package org.echoiot.server.transport.lwm2m.server.ota.firmware;

import org.jetbrains.annotations.NotNull;

public enum FirmwareDeliveryMethod {
    PULL(0, "Pull only"),
    PUSH(1, "Push only"),
    BOTH(2, "Push or Push");

    public int code;
    public String type;

    FirmwareDeliveryMethod(int code, String type) {
        this.code = code;
        this.type = type;
    }

    @NotNull
    public static FirmwareDeliveryMethod fromStateFwByType(String type) {
        for (@NotNull FirmwareDeliveryMethod to : FirmwareDeliveryMethod.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery type  : %s", type));
    }

    @NotNull
    public static FirmwareDeliveryMethod fromStateFwByCode(int code) {
        for (@NotNull FirmwareDeliveryMethod to : FirmwareDeliveryMethod.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW delivery code : %s", code));
    }
}
