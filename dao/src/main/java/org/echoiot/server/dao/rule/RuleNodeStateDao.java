package org.echoiot.server.dao.rule;

import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.dao.Dao;

import java.util.UUID;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleNodeStateDao extends Dao<RuleNodeState> {

    PageData<RuleNodeState> findByRuleNodeId(UUID ruleNodeId, PageLink pageLink);

    RuleNodeState findByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId);

    void removeByRuleNodeId(UUID ruleNodeId);

    void removeByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId);
}
