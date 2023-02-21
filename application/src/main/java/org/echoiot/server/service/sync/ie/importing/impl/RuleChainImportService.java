package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.data.sync.ie.RuleChainExportData;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.rule.RuleNodeDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.RULE_CHAIN, EntityType.DEVICE, EntityType.ASSET));

    @NotNull
    private final RuleChainService ruleChainService;
    @NotNull
    private final RuleNodeDao ruleNodeDao;

    @Override
    protected void setOwner(TenantId tenantId, @NotNull RuleChain ruleChain, IdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    @Nullable
    @Override
    protected RuleChain findExistingEntity(@NotNull EntitiesImportCtx ctx, @NotNull RuleChain ruleChain, IdProvider idProvider) {
        @Nullable RuleChain existingRuleChain = super.findExistingEntity(ctx, ruleChain, idProvider);
        if (existingRuleChain == null && ctx.isFindExistingByName()) {
            existingRuleChain = ruleChainService.findTenantRuleChainsByTypeAndName(ctx.getTenantId(), ruleChain.getType(), ruleChain.getName()).stream().findFirst().orElse(null);
        }
        return existingRuleChain;
    }

    @NotNull
    @Override
    protected RuleChain prepare(@NotNull EntitiesImportCtx ctx, @NotNull RuleChain ruleChain, @Nullable RuleChain old, @NotNull RuleChainExportData exportData, @NotNull IdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        @NotNull List<RuleNode> ruleNodes = Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList());
        if (old != null) {
            @NotNull List<RuleNodeId> nodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
            List<RuleNode> existing = ruleNodeDao.findByExternalIds(old.getId(), nodeIds);
            existing.forEach(node -> ctx.putInternalId(node.getExternalId(), node.getId()));
            ruleNodes.forEach(node -> {
                node.setRuleChainId(old.getId());
                node.setExternalId(node.getId());
                node.setId((RuleNodeId) ctx.getInternalId(node.getId()));
            });
        } else {
            ruleNodes.forEach(node -> {
                node.setRuleChainId(null);
                node.setExternalId(node.getId());
                node.setId(null);
            });
        }

        ruleNodes.forEach(ruleNode -> replaceIdsRecursively(ctx, idProvider, ruleNode.getConfiguration(), Collections.emptySet(), HINTS));
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.getInternalId(ruleChainConnectionInfo.getTargetRuleChainId(), false));
                });
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId((RuleNodeId) ctx.getInternalId(ruleChain.getFirstRuleNodeId()));
        }
        return ruleChain;
    }

    @Override
    protected RuleChain saveOrUpdate(@NotNull EntitiesImportCtx ctx, RuleChain ruleChain, @NotNull RuleChainExportData exportData, IdProvider idProvider) {
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            exportData.getMetaData().setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), exportData.getMetaData());
            return ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChain.getId());
        } else {
            return ruleChain;
        }
    }

    @Override
    protected boolean compare(@NotNull EntitiesImportCtx ctx, @NotNull RuleChainExportData exportData, @NotNull RuleChain prepared, RuleChain existing) {
        boolean different = super.compare(ctx, exportData, prepared, existing);
        if (!different) {
            RuleChainMetaData newMD = exportData.getMetaData();
            @Nullable RuleChainMetaData existingMD = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), prepared.getId());
            existingMD.setRuleChainId(null);
            different = !newMD.equals(existingMD);
        }
        return different;
    }

    @Override
    protected void onEntitySaved(@NotNull User user, @NotNull RuleChain savedRuleChain, @Nullable RuleChain oldRuleChain) throws EchoiotException {
        entityActionService.logEntityAction(user, savedRuleChain.getId(), savedRuleChain, null,
                oldRuleChain == null ? ActionType.ADDED : ActionType.UPDATED, null);
        if (savedRuleChain.getType() == RuleChainType.CORE) {
            clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedRuleChain.getId(),
                    oldRuleChain == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        } else if (savedRuleChain.getType() == RuleChainType.EDGE && oldRuleChain != null) {
            entityActionService.sendEntityNotificationMsgToEdge(user.getTenantId(), savedRuleChain.getId(), EdgeEventActionType.UPDATED);
        }
    }

    @NotNull
    @Override
    protected RuleChain deepCopy(@NotNull RuleChain ruleChain) {
        return new RuleChain(ruleChain);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
