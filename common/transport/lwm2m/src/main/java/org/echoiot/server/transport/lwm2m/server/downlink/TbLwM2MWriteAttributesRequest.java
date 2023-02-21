package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.echoiot.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.jetbrains.annotations.NotNull;

public class TbLwM2MWriteAttributesRequest extends AbstractTbLwM2MTargetedDownlinkRequest<WriteAttributesResponse> {

    @Getter
    private final ObjectAttributes attributes;

    @Builder
    private TbLwM2MWriteAttributesRequest(String versionedId, long timeout, ObjectAttributes attributes) {
        super(versionedId, timeout);
        this.attributes = attributes;
    }

    @NotNull
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.WRITE_ATTRIBUTES;
    }



}
