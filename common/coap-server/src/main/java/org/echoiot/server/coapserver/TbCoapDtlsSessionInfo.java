package org.echoiot.server.coapserver;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;

@Data
public class TbCoapDtlsSessionInfo {

    private ValidateDeviceCredentialsResponse msg;
    private DeviceProfile deviceProfile;
    private long lastActivityTime;


    public TbCoapDtlsSessionInfo(ValidateDeviceCredentialsResponse msg, DeviceProfile deviceProfile) {
        this.msg = msg;
        this.deviceProfile = deviceProfile;
        this.lastActivityTime = System.currentTimeMillis();
    }
}
