package org.echoiot.rule.engine.filter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.echoiot.common.util.DonAsynchron.withCallback;

/**
 * Created by Echo on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check relation",
        configClazz = TbCheckRelationNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Checks the presence of the relation between the originator of the message and other entities.",
        nodeDetails = "If 'check relation to specific entity' is selected, one must specify a related entity. " +
                "Otherwise, the rule node checks the presence of a relation to any entity that matches the direction and relation type criteria.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckRelationConfig")
public class TbCheckRelationNode implements TbNode {

    private TbCheckRelationNodeConfiguration config;
    private EntityId singleEntityId;

    @Override
    public void init(@NotNull TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckRelationNodeConfiguration.class);
        if (config.isCheckForSingleEntity()) {
            this.singleEntityId = EntityIdFactory.getByTypeAndId(config.getEntityType(), config.getEntityId());
            ctx.checkTenantEntity(singleEntityId);
        }
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws TbNodeException {
        ListenableFuture<Boolean> checkRelationFuture;
        if (config.isCheckForSingleEntity()) {
            checkRelationFuture = processSingle(ctx, msg);
        } else {
            checkRelationFuture = processList(ctx, msg);
        }
        withCallback(checkRelationFuture, filterResult -> ctx.tellNext(msg, filterResult ? "True" : "False"), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSingle(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        EntityId from;
        EntityId to;
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            from = singleEntityId;
            to = msg.getOriginator();
        } else {
            to = singleEntityId;
            from = msg.getOriginator();
        }
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), from, to, config.getRelationType(), RelationTypeGroup.COMMON);
    }

    @NotNull
    private ListenableFuture<Boolean> processList(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        if (EntitySearchDirection.FROM.name().equals(config.getDirection())) {
            return Futures.transformAsync(ctx.getRelationService()
                    .findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON), this::isEmptyList, MoreExecutors.directExecutor());
        } else {
            return Futures.transformAsync(ctx.getRelationService()
                    .findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), config.getRelationType(), RelationTypeGroup.COMMON), this::isEmptyList, MoreExecutors.directExecutor());
        }
    }

    @NotNull
    private ListenableFuture<Boolean> isEmptyList(@NotNull List<EntityRelation> entityRelations) {
        if (entityRelations.isEmpty()) {
            return Futures.immediateFuture(false);
        } else {
            return Futures.immediateFuture(true);
        }
    }

}
