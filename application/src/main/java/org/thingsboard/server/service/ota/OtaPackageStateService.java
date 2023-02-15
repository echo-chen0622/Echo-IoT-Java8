package org.thingsboard.server.service.ota;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;

public interface OtaPackageStateService {

    void update(Device device, Device oldDevice);

    void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged);

    boolean process(ToOtaPackageStateServiceMsg msg);

}
