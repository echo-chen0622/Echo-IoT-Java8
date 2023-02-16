package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.ResponseCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public class RpcLinkSetCallback<R, T> extends RpcDownlinkRequestCallbackProxy<R, T> {

    public RpcLinkSetCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(T response) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.CONTENT.getName()).value(JacksonUtil.toString(response)).build());
    }

}
