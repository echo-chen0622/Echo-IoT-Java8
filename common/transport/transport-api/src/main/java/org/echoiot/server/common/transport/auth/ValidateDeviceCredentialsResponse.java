package org.echoiot.server.common.transport.auth;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.DeviceProfile;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
@Builder
public class ValidateDeviceCredentialsResponse implements DeviceProfileAware, Serializable {

    @NotNull
    private final TransportDeviceInfo deviceInfo;
    @NotNull
    private final DeviceProfile deviceProfile;
    @NotNull
    private final String credentials;

    public boolean hasDeviceInfo() {
        return deviceInfo != null;
    }
}
