package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.eclipse.leshan.core.response.ReadResponse;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.jetbrains.annotations.NotNull;

public class TbLwM2MDeleteRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ReadResponse> {

    @Builder
    private TbLwM2MDeleteRequest(String versionedId, long timeout) {
        super(versionedId, timeout);
    }

    @NotNull
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.DELETE;
    }



}
