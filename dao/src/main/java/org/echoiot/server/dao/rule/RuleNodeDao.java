package org.echoiot.server.dao.rule;

import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.dao.Dao;

import java.util.List;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleNodeDao extends Dao<RuleNode> {

    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String search);

    PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink);

    List<RuleNode> findByExternalIds(RuleChainId ruleChainId, List<RuleNodeId> externalIds);

    void deleteByIdIn(List<RuleNodeId> ruleNodeIds);
}
