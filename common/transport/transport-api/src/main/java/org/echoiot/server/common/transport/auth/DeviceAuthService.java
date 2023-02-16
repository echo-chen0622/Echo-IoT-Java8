package org.echoiot.server.common.transport.auth;

import org.echoiot.server.common.data.security.DeviceCredentialsFilter;

public interface DeviceAuthService {

    DeviceAuthResult process(DeviceCredentialsFilter credentials);

}
