package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RpcEmptyResponseCallback<R extends LwM2mRequest<T>, T extends LwM2mResponse> extends RpcLwM2MDownlinkCallback<R, T> {

    public RpcEmptyResponseCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @NotNull
    protected Optional<String> serializeSuccessfulResponse(T response) {
        return Optional.empty();
    }

}
