package org.echoiot.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;

@Slf4j
public class TbLwM2MObserveCallback extends TbLwM2MUplinkTargetedCallback<ObserveRequest, ObserveResponse> {

    public TbLwM2MObserveCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    @Override
    public void onSuccess(ObserveRequest request, ObserveResponse response) {
        super.onSuccess(request, response);
        handler.onUpdateValueAfterReadResponse(client.getRegistration(), versionedId, response);
    }
}
