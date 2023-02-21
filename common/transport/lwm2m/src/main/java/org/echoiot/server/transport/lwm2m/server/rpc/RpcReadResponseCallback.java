package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.contentToString;

public class RpcReadResponseCallback<R extends LwM2mRequest<T>, T extends ReadResponse> extends RpcLwM2MDownlinkCallback<R, T> {

    public RpcReadResponseCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected Optional<String> serializeSuccessfulResponse(@NotNull T response) {
        return contentToString(response.getContent());
    }
}
