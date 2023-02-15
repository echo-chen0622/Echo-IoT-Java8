package org.thingsboard.server.service.edge.rpc.fetch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;

import static org.thingsboard.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@AllArgsConstructor
public class RuleChainsEdgeEventFetcher extends BasePageableEdgeEventFetcher<RuleChain> {

    private final RuleChainService ruleChainService;

    @Override
    PageData<RuleChain> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, RuleChain ruleChain) {
        ObjectNode isRootBody = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        boolean isRoot = false;
        try {
            isRoot = ruleChain.getId().equals(edge.getRootRuleChainId());
        } catch (Exception ignored) {}
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, isRoot);
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN,
                EdgeEventActionType.ADDED, ruleChain.getId(), isRootBody);
    }
}
