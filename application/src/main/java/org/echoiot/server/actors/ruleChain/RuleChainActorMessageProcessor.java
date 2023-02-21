package org.echoiot.server.actors.ruleChain;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.actors.TbActorCtx;
import org.echoiot.server.actors.TbActorRef;
import org.echoiot.server.actors.TbEntityActorId;
import org.echoiot.server.actors.service.DefaultActorService;
import org.echoiot.server.actors.shared.ComponentMsgProcessor;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.plugin.ComponentLifecycleState;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.plugin.ComponentLifecycleMsg;
import org.echoiot.server.common.msg.plugin.RuleNodeUpdatedMsg;
import org.echoiot.server.common.msg.queue.*;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.common.MultipleTbQueueTbMsgCallbackWrapper;
import org.echoiot.server.queue.common.TbQueueTbMsgCallbackWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class RuleChainActorMessageProcessor extends ComponentMsgProcessor<RuleChainId> {

    private static final String NA_RELATION_TYPE = "";
    private final TbActorRef parent;
    private final TbActorRef self;
    @NotNull
    private final Map<RuleNodeId, RuleNodeCtx> nodeActors;
    @NotNull
    private final Map<RuleNodeId, List<RuleNodeRelation>> nodeRoutes;
    private final RuleChainService service;
    private final TbClusterService clusterService;
    private final TbApiUsageReportClient apiUsageClient;
    private String ruleChainName;

    private RuleNodeId firstId;
    private RuleNodeCtx firstNode;
    private boolean started;

    RuleChainActorMessageProcessor(TenantId tenantId, @NotNull RuleChain ruleChain, @NotNull ActorSystemContext systemContext, TbActorRef parent, TbActorRef self) {
        super(systemContext, tenantId, ruleChain.getId());
        this.apiUsageClient = systemContext.getApiUsageClient();
        this.ruleChainName = ruleChain.getName();
        this.parent = parent;
        this.self = self;
        this.nodeActors = new HashMap<>();
        this.nodeRoutes = new HashMap<>();
        this.service = systemContext.getRuleChainService();
        this.clusterService = systemContext.getClusterService();
    }

    @Nullable
    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public void start(@NotNull TbActorCtx context) {
        if (!started) {
            RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
            if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
                List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
                log.trace("[{}][{}] Starting rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
                // Creating and starting the actors;
                for (@NotNull RuleNode ruleNode : ruleNodeList) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                }
                initRoutes(ruleChain, ruleNodeList);
                started = true;
            }
        } else {
            onUpdate(context);
        }
    }

    @Override
    public void onUpdate(@NotNull TbActorCtx context) {
        RuleChain ruleChain = service.findRuleChainById(tenantId, entityId);
        if (ruleChain != null && RuleChainType.CORE.equals(ruleChain.getType())) {
            ruleChainName = ruleChain.getName();
            List<RuleNode> ruleNodeList = service.getRuleChainNodes(tenantId, entityId);
            log.trace("[{}][{}] Updating rule chain with {} nodes", tenantId, entityId, ruleNodeList.size());
            for (@NotNull RuleNode ruleNode : ruleNodeList) {
                RuleNodeCtx existing = nodeActors.get(ruleNode.getId());
                if (existing == null) {
                    log.trace("[{}][{}] Creating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    TbActorRef ruleNodeActor = createRuleNodeActor(context, ruleNode);
                    nodeActors.put(ruleNode.getId(), new RuleNodeCtx(tenantId, self, ruleNodeActor, ruleNode));
                } else {
                    log.trace("[{}][{}] Updating rule node [{}]: {}", entityId, ruleNode.getId(), ruleNode.getName(), ruleNode);
                    existing.setSelf(ruleNode);
                    existing.getSelfActor().tellWithHighPriority(new RuleNodeUpdatedMsg(tenantId, existing.getSelf().getId()));
                }
            }

            @NotNull Set<RuleNodeId> existingNodes = ruleNodeList.stream().map(RuleNode::getId).collect(Collectors.toSet());
            @NotNull List<RuleNodeId> removedRules = nodeActors.keySet().stream().filter(node -> !existingNodes.contains(node)).collect(Collectors.toList());
            removedRules.forEach(ruleNodeId -> {
                log.trace("[{}][{}] Removing rule node [{}]", tenantId, entityId, ruleNodeId);
                RuleNodeCtx removed = nodeActors.remove(ruleNodeId);
                removed.getSelfActor().tellWithHighPriority(new ComponentLifecycleMsg(tenantId, removed.getSelf().getId(), ComponentLifecycleEvent.DELETED));
            });

            initRoutes(ruleChain, ruleNodeList);
        }
    }

    @Override
    public void stop(@NotNull TbActorCtx ctx) {
        log.trace("[{}][{}] Stopping rule chain with {} nodes", tenantId, entityId, nodeActors.size());
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).map(TbActorRef::getActorId).forEach(ctx::stop);
        nodeActors.clear();
        nodeRoutes.clear();
        started = false;
    }

    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) {
        nodeActors.values().stream().map(RuleNodeCtx::getSelfActor).forEach(actorRef -> actorRef.tellWithHighPriority(msg));
    }

    private TbActorRef createRuleNodeActor(@NotNull TbActorCtx ctx, @NotNull RuleNode ruleNode) {
        return ctx.getOrCreateChildActor(new TbEntityActorId(ruleNode.getId()),
                () -> DefaultActorService.RULE_DISPATCHER_NAME,
                () -> new RuleNodeActor.ActorCreator(systemContext, tenantId, entityId, ruleChainName, ruleNode.getId()));
    }

    private void initRoutes(@NotNull RuleChain ruleChain, @NotNull List<RuleNode> ruleNodeList) {
        nodeRoutes.clear();
        // Populating the routes map;
        for (@NotNull RuleNode ruleNode : ruleNodeList) {
            List<EntityRelation> relations = service.getRuleNodeRelations(TenantId.SYS_TENANT_ID, ruleNode.getId());
            log.trace("[{}][{}][{}] Processing rule node relations [{}]", tenantId, entityId, ruleNode.getId(), relations.size());
            if (relations.size() == 0) {
                nodeRoutes.put(ruleNode.getId(), Collections.emptyList());
            } else {
                for (@NotNull EntityRelation relation : relations) {
                    log.trace("[{}][{}][{}] Processing rule node relation [{}]", tenantId, entityId, ruleNode.getId(), relation.getTo());
                    if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                        RuleNodeCtx ruleNodeCtx = nodeActors.get(new RuleNodeId(relation.getTo().getId()));
                        if (ruleNodeCtx == null) {
                            throw new IllegalArgumentException("Rule Node [" + relation.getFrom() + "] has invalid relation to Rule node [" + relation.getTo() + "]");
                        }
                    }
                    nodeRoutes.computeIfAbsent(ruleNode.getId(), k -> new ArrayList<>())
                            .add(new RuleNodeRelation(ruleNode.getId(), relation.getTo(), relation.getType()));
                }
            }
        }

        firstId = ruleChain.getFirstRuleNodeId();
        firstNode = nodeActors.get(firstId);
        state = ComponentLifecycleState.ACTIVE;
    }

    void onQueueToRuleEngineMsg(@NotNull QueueToRuleEngineMsg envelope) {
        TbMsg msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        log.trace("[{}][{}] Processing message [{}]: {}", entityId, firstId, msg.getId(), msg);
        if (envelope.getRelationTypes() == null || envelope.getRelationTypes().isEmpty()) {
            onTellNext(msg, true);
        } else {
            onTellNext(msg, envelope.getMsg().getRuleNodeId(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    private void onTellNext(@NotNull TbMsg msg, boolean useRuleNodeIdFromMsg) {
        try {
            checkComponentStateActive(msg);
            RuleNodeId targetId = useRuleNodeIdFromMsg ? msg.getRuleNodeId() : null;
            RuleNodeCtx targetCtx;
            if (targetId == null) {
                targetCtx = firstNode;
                msg = msg.copyWithRuleChainId(entityId);
            } else {
                targetCtx = nodeActors.get(targetId);
            }
            if (targetCtx != null) {
                log.trace("[{}][{}] Pushing message to target rule node", entityId, targetId);
                pushMsgToNode(targetCtx, msg, NA_RELATION_TYPE);
            } else {
                log.trace("[{}][{}] Rule node does not exist. Probably old message", entityId, targetId);
                msg.getCallback().onSuccess();
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            msg.getCallback().onFailure(new RuleEngineException(e.getMessage()));
        }
    }

    public void onRuleChainInputMsg(@NotNull RuleChainInputMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            onTellNext(envelope.getMsg(), false);
        } else {
            parent.tell(envelope);
        }
    }

    public void onRuleChainOutputMsg(@NotNull RuleChainOutputMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        if (entityId.equals(envelope.getRuleChainId())) {
            var originatorNodeId = envelope.getTargetRuleNodeId();
            RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
            if (ruleNodeCtx != null && ruleNodeCtx.getSelf().isDebugMode()) {
                systemContext.persistDebugOutput(tenantId, originatorNodeId, envelope.getMsg(), envelope.getRelationType());
            }
            onTellNext(envelope.getMsg(), originatorNodeId, Collections.singleton(envelope.getRelationType()), RuleNodeException.UNKNOWN);
        } else {
            parent.tell(envelope);
        }
    }

    void onRuleChainToRuleChainMsg(@NotNull RuleChainToRuleChainMsg envelope) {
        var msg = envelope.getMsg();
        if (!checkMsgValid(msg)) {
            return;
        }
        try {
            checkComponentStateActive(envelope.getMsg());
            if (firstNode != null) {
                pushMsgToNode(firstNode, envelope.getMsg(), envelope.getFromRelationType());
            } else {
                envelope.getMsg().getCallback().onSuccess();
            }
        } catch (RuleNodeException e) {
            log.debug("Rule Chain is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
        }
    }

    void onTellNext(@NotNull RuleNodeToRuleChainTellNextMsg envelope) {
        var msg = envelope.getMsg();
        if (checkMsgValid(msg)) {
            onTellNext(msg, envelope.getOriginator(), envelope.getRelationTypes(), envelope.getFailureMessage());
        }
    }

    private void onTellNext(@NotNull TbMsg msg, @NotNull RuleNodeId originatorNodeId, @NotNull Set<String> relationTypes, String failureMessage) {
        try {
            checkComponentStateActive(msg);
            EntityId entityId = msg.getOriginator();
            TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, msg.getQueueName(), tenantId, entityId);

            List<RuleNodeRelation> ruleNodeRelations = nodeRoutes.get(originatorNodeId);
            if (ruleNodeRelations == null) { // When unchecked, this will cause NullPointerException when rule node doesn't exist anymore
                log.warn("[{}][{}][{}] No outbound relations (null). Probably rule node does not exist. Probably old message.", tenantId, entityId, msg.getId());
                ruleNodeRelations = Collections.emptyList();
            }

            @NotNull List<RuleNodeRelation> relationsByTypes = ruleNodeRelations.stream()
                                                                                .filter(r -> contains(relationTypes, r.getType()))
                                                                                .collect(Collectors.toList());
            int relationsCount = relationsByTypes.size();
            if (relationsCount == 0) {
                log.trace("[{}][{}][{}] No outbound relations to process", tenantId, entityId, msg.getId());
                if (relationTypes.contains(TbRelationTypes.FAILURE)) {
                    RuleNodeCtx ruleNodeCtx = nodeActors.get(originatorNodeId);
                    if (ruleNodeCtx != null) {
                        msg.getCallback().onFailure(new RuleNodeException(failureMessage, ruleChainName, ruleNodeCtx.getSelf()));
                    } else {
                        log.debug("[{}] Failure during message processing by Rule Node [{}]. Enable and see debug events for more info", entityId, originatorNodeId.getId());
                        msg.getCallback().onFailure(new RuleEngineException("Failure during message processing by Rule Node [" + originatorNodeId.getId().toString() + "]"));
                    }
                } else {
                    msg.getCallback().onSuccess();
                }
            } else if (relationsCount == 1) {
                for (@NotNull RuleNodeRelation relation : relationsByTypes) {
                    log.trace("[{}][{}][{}] Pushing message to single target: [{}]", tenantId, entityId, msg.getId(), relation.getOut());
                    pushToTarget(tpi, msg, relation.getOut(), relation.getType());
                }
            } else {
                @NotNull MultipleTbQueueTbMsgCallbackWrapper callbackWrapper = new MultipleTbQueueTbMsgCallbackWrapper(relationsCount, msg.getCallback());
                log.trace("[{}][{}][{}] Pushing message to multiple targets: [{}]", tenantId, entityId, msg.getId(), relationsByTypes);
                for (@NotNull RuleNodeRelation relation : relationsByTypes) {
                    EntityId target = relation.getOut();
                    putToQueue(tpi, msg, callbackWrapper, target);
                }
            }
        } catch (RuleNodeException rne) {
            msg.getCallback().onFailure(rne);
        } catch (Exception e) {
            log.warn("[" + tenantId + "]" + "[" + entityId + "]" + "[" + msg.getId() + "]" + " onTellNext failure", e);
            msg.getCallback().onFailure(new RuleEngineException("onTellNext - " + e.getMessage()));
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, @NotNull TbMsg msg, TbQueueCallback callbackWrapper, @NotNull EntityId target) {
        switch (target.getEntityType()) {
            case RULE_NODE:
                putToQueue(tpi, msg.copyWithRuleNodeId(entityId, new RuleNodeId(target.getId()), UUID.randomUUID()), callbackWrapper);
                break;
            case RULE_CHAIN:
                putToQueue(tpi, msg.copyWithRuleChainId(new RuleChainId(target.getId()), UUID.randomUUID()), callbackWrapper);
                break;
        }
    }

    private void pushToTarget(@NotNull TopicPartitionInfo tpi, @NotNull TbMsg msg, @NotNull EntityId target, String fromRelationType) {
        if (tpi.isMyPartition()) {
            switch (target.getEntityType()) {
                case RULE_NODE:
                    pushMsgToNode(nodeActors.get(new RuleNodeId(target.getId())), msg, fromRelationType);
                    break;
                case RULE_CHAIN:
                    parent.tell(new RuleChainToRuleChainMsg(new RuleChainId(target.getId()), entityId, msg, fromRelationType));
                    break;
            }
        } else {
            putToQueue(tpi, msg, new TbQueueTbMsgCallbackWrapper(msg.getCallback()), target);
        }
    }

    private void putToQueue(TopicPartitionInfo tpi, @NotNull TbMsg newMsg, TbQueueCallback callbackWrapper) {
        ToRuleEngineMsg toQueueMsg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(newMsg))
                .build();
        clusterService.pushMsgToRuleEngine(tpi, newMsg.getId(), toQueueMsg, callbackWrapper);
    }

    private boolean contains(@Nullable Set<String> relationTypes, String type) {
        if (relationTypes == null) {
            return true;
        }
        for (@NotNull String relationType : relationTypes) {
            if (relationType.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private void pushMsgToNode(@Nullable RuleNodeCtx nodeCtx, @NotNull TbMsg msg, String fromRelationType) {
        if (nodeCtx != null) {
            nodeCtx.getSelfActor().tell(new RuleChainToRuleNodeMsg(new DefaultTbContext(systemContext, ruleChainName, nodeCtx), msg, fromRelationType));
        } else {
            log.error("[{}][{}] RuleNodeCtx is empty", entityId, ruleChainName);
            msg.getCallback().onFailure(new RuleEngineException("Rule Node CTX is empty"));
        }
    }

    @NotNull
    @Override
    protected RuleNodeException getInactiveException() {
        RuleNode firstRuleNode = firstNode != null ? firstNode.getSelf() : null;
        return new RuleNodeException("Rule Chain is not active!  Failed to initialize.", ruleChainName, firstRuleNode);
    }

}
