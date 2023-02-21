package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.jetbrains.annotations.NotNull;

public class TbLwM2MCancelAllRequest implements TbLwM2MDownlinkRequest<Integer> {

    @Getter
    private final long timeout;

    @Builder
    private TbLwM2MCancelAllRequest(long timeout) {
        this.timeout = timeout;
    }

    @NotNull
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE_CANCEL_ALL;
    }

}
