package org.echoiot.server.common.data.device.profile.lwm2m.bootstrap;

import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.jetbrains.annotations.NotNull;

public class NoSecLwM2MBootstrapServerCredential extends AbstractLwM2MBootstrapServerCredential {

    private static final long serialVersionUID = 5540417758424747066L;

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
