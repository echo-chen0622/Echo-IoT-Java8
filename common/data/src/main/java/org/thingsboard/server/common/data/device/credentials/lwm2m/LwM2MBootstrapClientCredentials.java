package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LwM2MBootstrapClientCredentials {
    private LwM2MBootstrapClientCredential bootstrapServer;
    private LwM2MBootstrapClientCredential lwm2mServer;
}
