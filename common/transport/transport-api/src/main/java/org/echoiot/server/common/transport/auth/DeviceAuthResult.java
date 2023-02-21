package org.echoiot.server.common.transport.auth;

import org.echoiot.server.common.data.id.DeviceId;
import org.jetbrains.annotations.NotNull;

public class DeviceAuthResult {

    private final boolean success;
    private final DeviceId deviceId;
    private final String errorMsg;

    @NotNull
    public static DeviceAuthResult of(DeviceId deviceId) {
        return new DeviceAuthResult(true, deviceId, null);
    }

    @NotNull
    public static DeviceAuthResult of(String errorMsg) {
        return new DeviceAuthResult(false, null, errorMsg);
    }

    private DeviceAuthResult(boolean success, DeviceId deviceId, String errorMsg) {
        super();
        this.success = success;
        this.deviceId = deviceId;
        this.errorMsg = errorMsg;
    }

    public boolean isSuccess() {
        return success;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    @NotNull
    @Override
    public String toString() {
        return "DeviceAuthResult [success=" + success + ", deviceId=" + deviceId + ", errorMsg=" + errorMsg + "]";
    }

}
