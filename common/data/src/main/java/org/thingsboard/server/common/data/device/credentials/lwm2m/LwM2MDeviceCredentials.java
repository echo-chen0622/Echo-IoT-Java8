package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LwM2MDeviceCredentials {
    private LwM2MClientCredential client;
    private LwM2MBootstrapClientCredentials bootstrap;
}
