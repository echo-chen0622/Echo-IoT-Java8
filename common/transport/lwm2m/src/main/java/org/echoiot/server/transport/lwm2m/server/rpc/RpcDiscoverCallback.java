package org.echoiot.server.transport.lwm2m.server.rpc;

import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.eclipse.leshan.core.link.DefaultLinkSerializer;
import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RpcDiscoverCallback extends RpcLwM2MDownlinkCallback<DiscoverRequest, DiscoverResponse> {

    private final LinkSerializer serializer = new DefaultLinkSerializer();

    public RpcDiscoverCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @NotNull
    protected Optional<String> serializeSuccessfulResponse(@NotNull DiscoverResponse response) {
        return Optional.of(serializer.serialize(response.getObjectLinks()));
    }

}
