package org.echoiot.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.sync.ie.RuleChainExportData;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainExportService extends BaseEntityExportService<RuleChainId, RuleChain, RuleChainExportData> {

    @NotNull
    private final RuleChainService ruleChainService;

    @Override
    protected void setRelatedEntities(@NotNull EntitiesExportCtx<?> ctx, @NotNull RuleChain ruleChain, @NotNull RuleChainExportData exportData) {
        @Nullable RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), ruleChain.getId());
        Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList())
                .forEach(ruleNode -> {
                    ruleNode.setRuleChainId(null);
                    ctx.putExternalId(ruleNode.getId(), ruleNode.getExternalId());
                    ruleNode.setId(ctx.getExternalId(ruleNode.getId()));
                    ruleNode.setCreatedTime(0);
                    ruleNode.setExternalId(null);
                    replaceUuidsRecursively(ctx, ruleNode.getConfiguration(), Collections.emptySet());
                });
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(getExternalIdOrElseInternal(ctx, ruleChainConnectionInfo.getTargetRuleChainId()));
                });
        exportData.setMetaData(metaData);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId(ctx.getExternalId(ruleChain.getFirstRuleNodeId()));
        }
    }

    @NotNull
    @Override
    protected RuleChainExportData newExportData() {
        return new RuleChainExportData();
    }

    @NotNull
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.RULE_CHAIN);
    }

}
