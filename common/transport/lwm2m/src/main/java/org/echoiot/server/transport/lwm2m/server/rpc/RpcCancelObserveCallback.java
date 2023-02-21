package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.echoiot.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.eclipse.leshan.core.ResponseCode;
import org.jetbrains.annotations.NotNull;

public class RpcCancelObserveCallback extends RpcDownlinkRequestCallbackProxy<TbLwM2MCancelObserveRequest, Integer> {

    public RpcCancelObserveCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(@NotNull Integer response) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.CONTENT.getName()).value(response.toString()).build());
    }
}
