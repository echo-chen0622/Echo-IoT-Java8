package org.echoiot.server.actors.app;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.actors.*;
import org.echoiot.server.actors.device.SessionTimeoutCheckMsg;
import org.echoiot.server.actors.service.ContextAwareActor;
import org.echoiot.server.actors.service.ContextBasedCreator;
import org.echoiot.server.actors.service.DefaultActorService;
import org.echoiot.server.actors.tenant.TenantActor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageDataIterable;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.aware.TenantAwareMsg;
import org.echoiot.server.common.msg.edge.EdgeSessionMsg;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.queue.QueueToRuleEngineMsg;
import org.echoiot.server.common.msg.queue.RuleEngineException;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AppActor extends ContextAwareActor {

    private final TenantService tenantService;
    @NotNull
    private final Set<TenantId> deletedTenants;
    private volatile boolean ruleChainsInitialized;

    private AppActor(@NotNull ActorSystemContext systemContext) {
        super(systemContext);
        this.tenantService = systemContext.getTenantService();
        this.deletedTenants = new HashSet<>();
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        if (systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE)) {
            systemContext.schedulePeriodicMsgWithDelay(ctx, SessionTimeoutCheckMsg.instance(),
                                                       systemContext.getSessionReportTimeout(), systemContext.getSessionReportTimeout());
        }
    }

    @Override
    protected boolean doProcess(@NotNull TbActorMsg msg) {
        if (!ruleChainsInitialized) {
            initTenantActors();
            ruleChainsInitialized = true;
            if (msg.getMsgType() != MsgType.APP_INIT_MSG && msg.getMsgType() != MsgType.PARTITION_CHANGE_MSG) {
                log.warn("Rule Chains initialized by unexpected message: {}", msg);
            }
        }
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:
                break;
            case PARTITION_CHANGE_MSG:
                ctx.broadcastToChildren(msg);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, true);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                onToEdgeSessionMsg((EdgeSessionMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.TENANT);
                break;
            default:
                return false;
        }
        return true;
    }

    private void initTenantActors() {
        log.info("Starting main system actor.");
        try {
            if (systemContext.isTenantComponentsInitEnabled()) {
                @NotNull PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (@NotNull Tenant tenant : tenantIterator) {
                    log.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId());
                    log.debug("[{}] Tenant actor created.", tenant.getId());
                }
            }
            log.info("Main system actor started.");
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
    }

    private void onQueueToRuleEngineMsg(@NotNull QueueToRuleEngineMsg msg) {
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            msg.getMsg().getCallback().onFailure(new RuleEngineException("Message has system tenant id!"));
        } else {
            if (!deletedTenants.contains(msg.getTenantId())) {
                getOrCreateTenantActor(msg.getTenantId()).tell(msg);
            } else {
                msg.getMsg().getCallback().onSuccess();
            }
        }
    }

    private void onComponentLifecycleMsg(@NotNull ComponentLifecycleMsg msg) {
        @Nullable TbActorRef target = null;
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            if (!EntityType.TENANT_PROFILE.equals(msg.getEntityId().getEntityType())) {
                log.warn("Message has system tenant id: {}", msg);
            }
        } else {
            if (EntityType.TENANT.equals(msg.getEntityId().getEntityType())) {
                @NotNull TenantId tenantId = TenantId.fromUUID(msg.getEntityId().getId());
                if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                    log.info("[{}] Handling tenant deleted notification: {}", msg.getTenantId(), msg);
                    deletedTenants.add(tenantId);
                    ctx.stop(new TbEntityActorId(tenantId));
                } else {
                    target = getOrCreateTenantActor(msg.getTenantId());
                }
            } else {
                target = getOrCreateTenantActor(msg.getTenantId());
            }
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid component lifecycle msg: {}", msg.getTenantId(), msg);
        }
    }

    private void onToDeviceActorMsg(@NotNull TenantAwareMsg msg, boolean priority) {
        if (!deletedTenants.contains(msg.getTenantId())) {
            TbActorRef tenantActor = getOrCreateTenantActor(msg.getTenantId());
            if (priority) {
                tenantActor.tellWithHighPriority(msg);
            } else {
                tenantActor.tell(msg);
            }
        } else {
            if (msg instanceof TransportToDeviceActorMsgWrapper) {
                ((TransportToDeviceActorMsgWrapper) msg).getCallback().onSuccess();
            }
        }
    }

    private TbActorRef getOrCreateTenantActor(TenantId tenantId) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(tenantId),
                () -> DefaultActorService.TENANT_DISPATCHER_NAME,
                () -> new TenantActor.ActorCreator(systemContext, tenantId));
    }

    private void onToEdgeSessionMsg(@NotNull EdgeSessionMsg msg) {
        @Nullable TbActorRef target = null;
        if (ModelConstants.SYSTEM_TENANT.equals(msg.getTenantId())) {
            log.warn("Message has system tenant id: {}", msg);
        } else {
            target = getOrCreateTenantActor(msg.getTenantId());
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid edge session msg: {}", msg.getTenantId(), msg);
        }
    }

    public static class ActorCreator extends ContextBasedCreator {

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @NotNull
        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(TenantId.SYS_TENANT_ID);
        }

        @NotNull
        @Override
        public TbActor createActor() {
            return new AppActor(context);
        }
    }

}
