package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import lombok.Getter;
import org.echoiot.server.transport.lwm2m.server.LwM2MOperationType;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.CreateResponse;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TbLwM2MCreateRequest extends AbstractTbLwM2MTargetedDownlinkRequest<CreateResponse> {

    @Getter
    private final Object value;
    @Getter
    private final ContentFormat objectContentFormat;
    @Getter
    private final Map<String, Object> nodes;

    @Builder
    private TbLwM2MCreateRequest(String versionedId, long timeout, Object value, ContentFormat objectContentFormat, Map<String, Object> nodes) {
        super(versionedId, timeout);
        this.value = value;
        this.objectContentFormat = objectContentFormat;
        this.nodes = nodes;
    }

    @NotNull
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.CREATE;
    }
}
