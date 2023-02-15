package org.thingsboard.server.service.lwm2m;

import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;

public interface LwM2MService {

    LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer);

}
