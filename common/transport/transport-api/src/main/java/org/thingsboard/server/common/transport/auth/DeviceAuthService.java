package org.thingsboard.server.common.transport.auth;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentialsFilter;

import java.util.Optional;

public interface DeviceAuthService {

    DeviceAuthResult process(DeviceCredentialsFilter credentials);

}
