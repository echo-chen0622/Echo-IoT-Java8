package org.echoiot.server.common.transport.auth;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.DeviceProfile;

@Data
@Builder
public class GetOrCreateDeviceFromGatewayResponse implements DeviceProfileAware {

    private TransportDeviceInfo deviceInfo;
    private DeviceProfile deviceProfile;

}
