package org.echoiot.server.actors.tenant;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.actors.*;
import org.echoiot.server.actors.device.DeviceActorCreator;
import org.echoiot.server.actors.ruleChain.RuleChainManagerActor;
import org.echoiot.server.actors.service.ContextBasedCreator;
import org.echoiot.server.actors.service.DefaultActorService;
import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.aware.DeviceAwareMsg;
import org.echoiot.server.common.msg.aware.RuleChainAwareMsg;
import org.echoiot.server.common.msg.edge.EdgeSessionMsg;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;
import org.echoiot.server.common.msg.queue.QueueToRuleEngineMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.service.edge.rpc.EdgeRpcService;
import org.echoiot.server.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Slf4j
public class TenantActor extends RuleChainManagerActor {

    private boolean isRuleEngine;
    private boolean isCore;
    private ApiUsageState apiUsageState;

    private TenantActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
    }

    boolean cantFindTenant = false;

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}] Starting tenant actor.", tenantId);
        try {
            Tenant tenant = systemContext.getTenantService().findTenantById(tenantId);
            if (tenant == null) {
                cantFindTenant = true;
                log.info("[{}] Started tenant actor for missing tenant.", tenantId);
            } else {
                @Nullable TenantProfile tenantProfile = systemContext.getTenantProfileCache().get(tenant.getTenantProfileId());

                isCore = systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE);
                isRuleEngine = systemContext.getServiceInfoProvider().isService(ServiceType.TB_RULE_ENGINE);
                if (isRuleEngine) {
                    try {
                        if (getApiUsageState().isReExecEnabled()) {
                            log.debug("[{}] Going to init rule chains", tenantId);
                            initRuleChains();
                        } else {
                            log.info("[{}] Skip init of the rule chains due to API limits", tenantId);
                        }
                    } catch (Exception e) {
                        cantFindTenant = true;
                    }
                }
                log.debug("[{}] Tenant actor started.", tenantId);
            }
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", tenantId, e);
        }
    }

    @Override
    public void destroy() {
        log.info("[{}] Stopping tenant actor.", tenantId);
    }

    @Override
    protected boolean doProcess(@NotNull TbActorMsg msg) {
        if (cantFindTenant) {
            log.info("[{}] Processing missing Tenant msg: {}", tenantId, msg);
            if (msg.getMsgType().equals(MsgType.QUEUE_TO_RULE_ENGINE_MSG)) {
                @NotNull QueueToRuleEngineMsg queueMsg = (QueueToRuleEngineMsg) msg;
                queueMsg.getMsg().getCallback().onSuccess();
            } else if (msg.getMsgType().equals(MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG)) {
                @NotNull TransportToDeviceActorMsgWrapper transportMsg = (TransportToDeviceActorMsgWrapper) msg;
                transportMsg.getCallback().onSuccess();
            }
            return true;
        }
        switch (msg.getMsgType()) {
            case PARTITION_CHANGE_MSG:
                @NotNull PartitionChangeMsg partitionChangeMsg = (PartitionChangeMsg) msg;
                ServiceType serviceType = partitionChangeMsg.getServiceType();
                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                    //To Rule Chain Actors
                    broadcast(msg);
                } else if (ServiceType.TB_CORE.equals(serviceType)) {
                    List<TbActorId> deviceActorIds = ctx.filterChildren(new TbEntityTypeActorIdPredicate(EntityType.DEVICE) {
                        @Override
                        protected boolean testEntityId(@NotNull EntityId entityId) {
                            return super.testEntityId(entityId) && !isMyPartition(entityId);
                        }
                    });
                    deviceActorIds.forEach(id -> ctx.stop(id));
                }
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((DeviceAwareMsg) msg, true);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.DEVICE);
                break;
            case RULE_CHAIN_INPUT_MSG:
            case RULE_CHAIN_OUTPUT_MSG:
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                onRuleChainMsg((RuleChainAwareMsg) msg);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                onToEdgeSessionMsg((EdgeSessionMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean isMyPartition(EntityId entityId) {
        return systemContext.resolve(ServiceType.TB_CORE, tenantId, entityId).isMyPartition();
    }

    private void onQueueToRuleEngineMsg(@NotNull QueueToRuleEngineMsg msg) {
        if (!isRuleEngine) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
            return;
        }
        TbMsg tbMsg = msg.getMsg();
        if (getApiUsageState().isReExecEnabled()) {
            if (tbMsg.getRuleChainId() == null) {
                if (getRootChainActor() != null) {
                    getRootChainActor().tell(msg);
                } else {
                    tbMsg.getCallback().onFailure(new RuleEngineException("No Root Rule Chain available!"));
                    log.info("[{}] No Root Chain: {}", tenantId, msg);
                }
            } else {
                try {
                    ctx.tell(new TbEntityActorId(tbMsg.getRuleChainId()), msg);
                } catch (TbActorNotRegisteredException ex) {
                    log.trace("Received message for non-existing rule chain: [{}]", tbMsg.getRuleChainId());
                    //TODO: 3.1 Log it to dead letters queue;
                    tbMsg.getCallback().onSuccess();
                }
            }
        } else {
            log.trace("[{}] Ack message because Rule Engine is disabled", tenantId);
            tbMsg.getCallback().onSuccess();
        }
    }

    private void onRuleChainMsg(@NotNull RuleChainAwareMsg msg) {
        if (getApiUsageState().isReExecEnabled()) {
            getOrCreateActor(msg.getRuleChainId()).tell(msg);
        }
    }

    private void onToDeviceActorMsg(@NotNull DeviceAwareMsg msg, boolean priority) {
        if (!isCore) {
            log.warn("RECEIVED INVALID MESSAGE: {}", msg);
        }
        TbActorRef deviceActor = getOrCreateDeviceActor(msg.getDeviceId());
        if (priority) {
            deviceActor.tellWithHighPriority(msg);
        } else {
            deviceActor.tell(msg);
        }
    }

    private void onComponentLifecycleMsg(@NotNull ComponentLifecycleMsg msg) {
        if (msg.getEntityId().getEntityType().equals(EntityType.API_USAGE_STATE)) {
            @NotNull ApiUsageState old = getApiUsageState();
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
            if (old.isReExecEnabled() && !apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to DISABLE Rule Engine execution.", tenantId);
                destroyRuleChains();
            } else if (!old.isReExecEnabled() && apiUsageState.isReExecEnabled()) {
                log.info("[{}] Received API state update. Going to ENABLE Rule Engine execution.", tenantId);
                initRuleChains();
            }
        } else if (msg.getEntityId().getEntityType() == EntityType.EDGE) {
            @NotNull EdgeId edgeId = new EdgeId(msg.getEntityId().getId());
            EdgeRpcService edgeRpcService = systemContext.getEdgeRpcService();
            if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                edgeRpcService.deleteEdge(tenantId, edgeId);
            } else if (msg.getEvent() == ComponentLifecycleEvent.UPDATED) {
                Edge edge = systemContext.getEdgeService().findEdgeById(tenantId, edgeId);
                edgeRpcService.updateEdge(tenantId, edge);
            }
        } else if (isRuleEngine) {
            TbActorRef target = getEntityActorRef(msg.getEntityId());
            if (target != null) {
                if (msg.getEntityId().getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChain ruleChain = systemContext.getRuleChainService().
                            findRuleChainById(tenantId, new RuleChainId(msg.getEntityId().getId()));
                    if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                        visit(ruleChain, target);
                    }
                }
                target.tellWithHighPriority(msg);
            } else {
                log.debug("[{}] Invalid component lifecycle msg: {}", tenantId, msg);
            }
        }
    }

    private TbActorRef getOrCreateDeviceActor(DeviceId deviceId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(deviceId),
                () -> DefaultActorService.DEVICE_DISPATCHER_NAME,
                () -> new DeviceActorCreator(systemContext, tenantId, deviceId));
    }

    private void onToEdgeSessionMsg(EdgeSessionMsg msg) {
        systemContext.getEdgeRpcService().onToEdgeSessionMsg(tenantId, msg);
    }

    @NotNull
    private ApiUsageState getApiUsageState() {
        if (apiUsageState == null) {
            apiUsageState = new ApiUsageState(systemContext.getApiUsageStateService().getApiUsageState(tenantId));
        }
        return apiUsageState;
    }

    public static class ActorCreator extends ContextBasedCreator {

        private final TenantId tenantId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId) {
            super(context);
            this.tenantId = tenantId;
        }

        @NotNull
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(tenantId);
        }

        @NotNull
        @Override
        public TbActor createActor() {
            return new TenantActor(context, tenantId);
        }
    }

}
