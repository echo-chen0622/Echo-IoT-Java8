package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.eclipse.leshan.core.response.ReadResponse;

public class TbLwM2MDeleteRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ReadResponse> {

    @Builder
    private TbLwM2MDeleteRequest(String versionedId, long timeout) {
        super(versionedId, timeout);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.DELETE;
    }



}
