package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;

public interface TbLwM2MDownlinkRequest<T> {

    LwM2MOperationType getType();

    long getTimeout();

}
