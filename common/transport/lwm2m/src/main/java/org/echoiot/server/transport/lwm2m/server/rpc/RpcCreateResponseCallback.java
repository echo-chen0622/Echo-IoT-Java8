package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RpcCreateResponseCallback<R extends LwM2mRequest<T>, T extends CreateResponse> extends RpcLwM2MDownlinkCallback<R, T> {

    public RpcCreateResponseCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @NotNull
    @Override
    protected Optional<String> serializeSuccessfulResponse(@NotNull T response) {
        @NotNull String value = response.getLocation() != null ? "location=" + response.getLocation() : "";
        return Optional.of(value);
    }
}
