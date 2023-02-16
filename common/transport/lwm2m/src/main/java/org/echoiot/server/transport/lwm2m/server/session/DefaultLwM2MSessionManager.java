package org.echoiot.server.transport.lwm2m.server.session;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.service.DefaultTransportService;
import org.echoiot.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.echoiot.server.transport.lwm2m.server.rpc.LwM2MRpcRequestHandler;
import org.echoiot.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class DefaultLwM2MSessionManager implements LwM2MSessionManager {

    private final TransportService transportService;
    private final LwM2MAttributesService attributesService;
    private final LwM2MRpcRequestHandler rpcHandler;
    private final LwM2mUplinkMsgHandler uplinkHandler;

    public DefaultLwM2MSessionManager(TransportService transportService,
                                      @Lazy LwM2MAttributesService attributesService,
                                      @Lazy LwM2MRpcRequestHandler rpcHandler,
                                      @Lazy LwM2mUplinkMsgHandler uplinkHandler) {
        this.transportService = transportService;
        this.attributesService = attributesService;
        this.rpcHandler = rpcHandler;
        this.uplinkHandler = uplinkHandler;
    }

    @Override
    public void register(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(uplinkHandler, attributesService, rpcHandler, sessionInfo, transportService));
        TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setSessionEvent(DefaultTransportService.SESSION_EVENT_MSG_OPEN)
                .setSubscribeToAttributes(DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                .setSubscribeToRPC(DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG)
                .build();
        transportService.process(msg, null);
    }

    @Override
    public void deregister(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, DefaultTransportService.SESSION_EVENT_MSG_CLOSED, null);
        transportService.deregisterSession(sessionInfo);
    }
}
