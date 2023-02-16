package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.DeviceTransportType;

import java.util.List;

@Data
public class Lwm2mDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private static final long serialVersionUID = 6257277825459600068L;

    private TelemetryMappingConfiguration observeAttr;
    private boolean bootstrapServerUpdateEnable;
    private List<LwM2MBootstrapServerCredential> bootstrap;
    private OtherConfiguration clientLwM2mSettings;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.LWM2M;
    }

}
