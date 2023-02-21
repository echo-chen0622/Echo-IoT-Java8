package org.echoiot.server.service.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.edge.TbEdgeService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.sync.ie.importing.csv.AbstractBulkImportService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeBulkImportService extends AbstractBulkImportService<Edge> {
    @NotNull
    private final EdgeService edgeService;
    @NotNull
    private final TbEdgeService tbEdgeService;
    @NotNull
    private final RuleChainService ruleChainService;

    @Override
    protected void setEntityFields(@NotNull Edge entity, @NotNull Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case ROUTING_KEY:
                    entity.setRoutingKey(value);
                    break;
                case SECRET:
                    entity.setSecret(value);
                    break;
            }
        });
        entity.setAdditionalInfo(additionalInfo);
    }

    @SneakyThrows
    @Override
    protected Edge saveEntity(@NotNull SecurityUser user, Edge entity, Map<BulkImportColumnType, String> fields) {
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        return tbEdgeService.save(entity, edgeTemplateRootRuleChain, user);
    }

    @NotNull
    @Override
    protected Edge findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(edgeService.findEdgeByTenantIdAndName(tenantId, name))
                .orElseGet(Edge::new);
    }

    @Override
    protected void setOwners(@NotNull Edge entity, @NotNull SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    @NotNull
    @Override
    protected EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
