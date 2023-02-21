package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.jetbrains.annotations.NotNull;

public class TbLwM2MCancelObserveRequest extends AbstractTbLwM2MTargetedDownlinkRequest<Integer> {

    @Builder
    private TbLwM2MCancelObserveRequest(String versionedId, long timeout) {
        super(versionedId, timeout);
    }

    @NotNull
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE_CANCEL;
    }



}
