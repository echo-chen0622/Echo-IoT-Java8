package org.thingsboard.server.coapserver;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

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
