package org.echoiot.server.transport.lwm2m.server.downlink;

import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;

public class TbLwM2MExecuteCallback extends TbLwM2MTargetedCallback<ExecuteRequest, ExecuteResponse> {

    public TbLwM2MExecuteCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(logService, client, targetId);
    }

}
