package org.echoiot.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.RuleNodeStateEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.rule.RuleNodeStateDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleNodeStateDao extends JpaAbstractDao<RuleNodeStateEntity, RuleNodeState> implements RuleNodeStateDao {

    @Resource
    private RuleNodeStateRepository ruleNodeStateRepository;

    @NotNull
    @Override
    protected Class<RuleNodeStateEntity> getEntityClass() {
        return RuleNodeStateEntity.class;
    }

    @Override
    protected JpaRepository<RuleNodeStateEntity, UUID> getRepository() {
        return ruleNodeStateRepository;
    }

    @NotNull
    @Override
    public PageData<RuleNodeState> findByRuleNodeId(UUID ruleNodeId, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeStateRepository.findByRuleNodeId(ruleNodeId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        return DaoUtil.getData(ruleNodeStateRepository.findByRuleNodeIdAndEntityId(ruleNodeId, entityId));
    }

    @Transactional
    @Override
    public void removeByRuleNodeId(UUID ruleNodeId) {
        ruleNodeStateRepository.removeByRuleNodeId(ruleNodeId);
    }

    @Transactional
    @Override
    public void removeByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        ruleNodeStateRepository.removeByRuleNodeIdAndEntityId(ruleNodeId, entityId);
    }
}
