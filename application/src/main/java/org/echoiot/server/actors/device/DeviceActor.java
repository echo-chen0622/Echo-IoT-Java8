package org.echoiot.server.actors.device;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.echoiot.rule.engine.api.msg.DeviceEdgeUpdateMsg;
import org.echoiot.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.actors.TbActorCtx;
import org.echoiot.server.actors.TbActorException;
import org.echoiot.server.actors.service.ContextAwareActor;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.echoiot.server.service.rpc.FromDeviceRpcResponseActorMsg;
import org.echoiot.server.service.rpc.RemoveRpcActorMsg;
import org.echoiot.server.service.rpc.ToDeviceRpcRequestActorMsg;
import org.echoiot.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

@Slf4j
public class DeviceActor extends ContextAwareActor {

    private final DeviceActorMessageProcessor processor;

    DeviceActor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}][{}] Starting device actor.", processor.tenantId, processor.deviceId);
        try {
            processor.init(ctx);
            log.debug("[{}][{}] Device actor started.", processor.tenantId, processor.deviceId);
        } catch (Exception e) {
            log.warn("[{}][{}] Unknown failure", processor.tenantId, processor.deviceId, e);
            throw new TbActorException("Failed to initialize device actor", e);
        }
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                processor.process(ctx, (TransportToDeviceActorMsgWrapper) msg);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processAttributesUpdate(ctx, (DeviceAttributesEventNotificationMsg) msg);
                break;
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processCredentialsUpdate(msg);
                break;
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processNameOrTypeUpdate((DeviceNameOrTypeUpdateMsg) msg);
                break;
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
                processor.processRpcRequest(ctx, (ToDeviceRpcRequestActorMsg) msg);
                break;
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                processor.processRpcResponsesFromEdge(ctx, (FromDeviceRpcResponseActorMsg) msg);
                break;
            case DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG:
                processor.processServerSideRpcTimeout(ctx, (DeviceActorServerSideRpcTimeoutMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                processor.checkSessionsTimeout();
                break;
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processEdgeUpdate((DeviceEdgeUpdateMsg) msg);
                break;
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                processor.processRemoveRpc(ctx, (RemoveRpcActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

}
