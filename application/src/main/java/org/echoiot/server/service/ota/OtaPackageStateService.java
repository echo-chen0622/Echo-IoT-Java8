package org.echoiot.server.service.ota;

import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;

public interface OtaPackageStateService {

    void update(Device device, Device oldDevice);

    void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged);

    boolean process(ToOtaPackageStateServiceMsg msg);

}
