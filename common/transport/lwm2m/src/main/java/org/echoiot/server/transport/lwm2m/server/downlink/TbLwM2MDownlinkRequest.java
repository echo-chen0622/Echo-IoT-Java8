package org.echoiot.server.transport.lwm2m.server.downlink;

import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;

public interface TbLwM2MDownlinkRequest<T> {

    LwM2MOperationType getType();

    long getTimeout();

}
