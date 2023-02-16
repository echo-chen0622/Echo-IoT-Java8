package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.echoiot.server.transport.lwm2m.server.downlink.TbLwM2MCancelAllRequest;
import org.eclipse.leshan.core.ResponseCode;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public class RpcCancelAllObserveCallback extends RpcDownlinkRequestCallbackProxy<TbLwM2MCancelAllRequest, Integer> {

    public RpcCancelAllObserveCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(Integer response) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.CONTENT.getName()).value(response.toString()).build());
    }
}
