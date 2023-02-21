package org.echoiot.server.service.edge.rpc.fetch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.dao.rule.RuleChainService;
import org.jetbrains.annotations.NotNull;

import static org.echoiot.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Slf4j
@AllArgsConstructor
public class RuleChainsEdgeEventFetcher extends BasePageableEdgeEventFetcher<RuleChain> {

    @NotNull
    private final RuleChainService ruleChainService;

    @Override
    PageData<RuleChain> fetchPageData(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) {
        return ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull RuleChain ruleChain) {
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
