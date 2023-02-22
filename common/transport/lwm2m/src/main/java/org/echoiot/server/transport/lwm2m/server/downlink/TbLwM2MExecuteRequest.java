package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.eclipse.leshan.core.response.ReadResponse;

public class TbLwM2MExecuteRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ReadResponse> {

    @Getter
    private final Object params;

    @Builder
    private TbLwM2MExecuteRequest(String versionedId, long timeout, Object params) {
        super(versionedId, timeout);
        this.params = params;
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.EXECUTE;
    }



}
