package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

import java.util.Optional;

public abstract class RpcLwM2MDownlinkCallback<R extends LwM2mRequest<T>, T extends LwM2mResponse> extends RpcDownlinkRequestCallbackProxy<R, T> {

    public RpcLwM2MDownlinkCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(T response) {
        LwM2MRpcResponseBody.LwM2MRpcResponseBodyBuilder builder = LwM2MRpcResponseBody.builder().result(response.getCode().getName());
        if (response.isSuccess()) {
            Optional<String> responseValue = serializeSuccessfulResponse(response);
            if (responseValue.isPresent() && StringUtils.isNotEmpty(responseValue.get())) {
                builder.value(responseValue.get());
            }
        } else {
            if (StringUtils.isNotEmpty(response.getErrorMessage())) {
                builder.error(response.getErrorMessage());
            }
        }
        reply(builder.build());
    }

    protected abstract Optional<String> serializeSuccessfulResponse(T response);
}
