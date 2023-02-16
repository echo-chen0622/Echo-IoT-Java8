package org.echoiot.server.service.lwm2m;

import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;

public interface LwM2MService {

    LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer);

}
