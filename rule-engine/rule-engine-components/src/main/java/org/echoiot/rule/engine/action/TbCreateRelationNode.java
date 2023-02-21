package org.echoiot.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.rule.engine.util.EntityContainer;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and (entity type pattern for Asset, Device) and then create a relation to Originator Entity by type and direction." +
                " If Selected entity type: Asset, Device or Customer will create new Entity if it doesn't exist and selected checkbox 'Create new entity if not exists'.<br>" +
                " In case that relation from the message originator to the selected entity not exist and  If selected checkbox 'Remove current relations'," +
                " before creating the new relation all existed relations to message originator by type and direction will be removed.<br>" +
                " If relation from the message originator to the selected entity created and If selected checkbox 'Change originator to related entity'," +
                " outbound message will be processed as a message from this entity.",
        nodeDetails = "If the relation already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle"
)
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(@NotNull TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @NotNull
    @Override
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull EntityContainer entity, String relationType) {
        @NotNull ListenableFuture<Boolean> future = createRelationIfAbsent(ctx, msg, entity, relationType);
        return Futures.transform(future, result -> {
            if (result && config.isChangeOriginatorToRelatedEntity()) {
                TbMsg tbMsg = ctx.transformMsg(msg, msg.getType(), entity.getEntityId(), msg.getMetaData(), msg.getData());
                return new RelationContainer(tbMsg, result);
            }
            return new RelationContainer(msg, result);
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> createRelationIfAbsent(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull EntityContainer entityContainer, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, entityContainer);
        return Futures.transformAsync(deleteCurrentRelationsIfNeeded(ctx, msg, sdId, relationType), v ->
                        checkRelationAndCreateIfAbsent(ctx, entityContainer, relationType, sdId),
                ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Void> deleteCurrentRelationsIfNeeded(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull SearchDirectionIds sdId, String relationType) {
        if (config.isRemoveCurrentRelations()) {
            return deleteOriginatorRelations(ctx, findOriginatorRelations(ctx, msg, sdId, relationType));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<List<EntityRelation>> findOriginatorRelations(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull SearchDirectionIds sdId, String relationType) {
        if (sdId.isOriginatorDirectionFrom()) {
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        } else {
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        }
    }

    @NotNull
    private ListenableFuture<Void> deleteOriginatorRelations(@NotNull TbContext ctx, @NotNull ListenableFuture<List<EntityRelation>> originatorRelationsFuture) {
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            @NotNull List<ListenableFuture<Boolean>> list = new ArrayList<>();
            if (!CollectionUtils.isEmpty(originatorRelations)) {
                for (EntityRelation relation : originatorRelations) {
                    list.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation));
                }
            }
            return Futures.transform(Futures.allAsList(list), result -> null, ctx.getDbCallbackExecutor());
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> checkRelationAndCreateIfAbsent(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, String relationType, @NotNull SearchDirectionIds sdId) {
        return Futures.transformAsync(checkRelation(ctx, sdId, relationType), relationPresent -> {
            if (relationPresent) {
                return Futures.immediateFuture(true);
            }
            return processCreateRelation(ctx, entityContainer, sdId, relationType);
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> checkRelation(@NotNull TbContext ctx, @NotNull SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
    }

    @NotNull
    private ListenableFuture<Boolean> processCreateRelation(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        switch (entityContainer.getEntityType()) {
            case ASSET:
                return processAsset(ctx, entityContainer, sdId, relationType);
            case DEVICE:
                return processDevice(ctx, entityContainer, sdId, relationType);
            case CUSTOMER:
                return processCustomer(ctx, entityContainer, sdId, relationType);
            case DASHBOARD:
                return processDashboard(ctx, entityContainer, sdId, relationType);
            case ENTITY_VIEW:
                return processView(ctx, entityContainer, sdId, relationType);
            case EDGE:
                return processEdge(ctx, entityContainer, sdId, relationType);
            case TENANT:
                return processTenant(ctx, entityContainer, sdId, relationType);
            case USER:
                return processUser(ctx, entityContainer, sdId, relationType);
        }
        return Futures.immediateFuture(true);
    }

    @NotNull
    private ListenableFuture<Boolean> processView(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(entityContainer.getEntityId().getId())), entityView -> {
            if (entityView != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processEdge(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(entityContainer.getEntityId().getId())), edge -> {
            if (edge != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processDevice(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(entityContainer.getEntityId().getId())), device -> {
            if (device != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processAsset(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(entityContainer.getEntityId().getId())), asset -> {
            if (asset != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processCustomer(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(entityContainer.getEntityId().getId())), customer -> {
            if (customer != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processDashboard(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(entityContainer.getEntityId().getId())), dashboard -> {
            if (dashboard != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processTenant(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), TenantId.fromUUID(entityContainer.getEntityId().getId())), tenant -> {
            if (tenant != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @NotNull
    private ListenableFuture<Boolean> processUser(@NotNull TbContext ctx, @NotNull EntityContainer entityContainer, @NotNull SearchDirectionIds sdId, String relationType) {
        return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(entityContainer.getEntityId().getId())), user -> {
            if (user != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Boolean> processSave(@NotNull TbContext ctx, @NotNull SearchDirectionIds sdId, String relationType) {
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON));
    }

}
