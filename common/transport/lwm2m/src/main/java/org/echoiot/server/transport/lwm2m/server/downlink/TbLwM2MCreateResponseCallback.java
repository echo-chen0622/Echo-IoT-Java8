package org.echoiot.server.transport.lwm2m.server.downlink;

import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.jetbrains.annotations.NotNull;

public class TbLwM2MCreateResponseCallback extends TbLwM2MUplinkTargetedCallback<CreateRequest, CreateResponse> {

    public TbLwM2MCreateResponseCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(@NotNull CreateRequest request, CreateResponse response) {
        super.onSuccess(request, response);
        handler.onCreateResponseOk(client, versionedId, request);
    }

}
