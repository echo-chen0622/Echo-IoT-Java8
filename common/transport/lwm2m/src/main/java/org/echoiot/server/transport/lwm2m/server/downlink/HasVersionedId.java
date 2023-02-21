package org.echoiot.server.transport.lwm2m.server.downlink;

import org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.jetbrains.annotations.Nullable;

public interface HasVersionedId {

    String getVersionedId();

    @Nullable
    default String getObjectId(){
        return LwM2MTransportUtil.fromVersionedIdToObjectId(getVersionedId());
    }

}
