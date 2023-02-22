package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.eclipse.leshan.core.response.DiscoverResponse;

public class TbLwM2MDiscoverRequest extends AbstractTbLwM2MTargetedDownlinkRequest<DiscoverResponse> {

    @Builder
    private TbLwM2MDiscoverRequest(String versionedId, long timeout) {
        super(versionedId, timeout);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.DISCOVER;
    }



}
