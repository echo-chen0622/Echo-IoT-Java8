package org.thingsboard.server.common.transport.auth;

import org.thingsboard.server.common.data.DeviceProfile;

public interface DeviceProfileAware {

    DeviceProfile getDeviceProfile();

}
