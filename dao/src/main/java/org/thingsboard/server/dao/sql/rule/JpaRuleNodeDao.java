package org.thingsboard.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlDao
public class JpaRuleNodeDao extends JpaAbstractSearchTextDao<RuleNodeEntity, RuleNode> implements RuleNodeDao {

    @Autowired
    private RuleNodeRepository ruleNodeRepository;

    @Override
    protected Class<RuleNodeEntity> getEntityClass() {
        return RuleNodeEntity.class;
    }

    @Override
    protected JpaRepository<RuleNodeEntity, UUID> getRepository() {
        return ruleNodeRepository;
    }

    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String search) {
        return DaoUtil.convertDataList(ruleNodeRepository.findRuleNodesByTenantIdAndType(tenantId.getId(), type, search));
    }

    @Override
    public PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeRepository
                .findAllRuleNodesByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<RuleNode> findByExternalIds(RuleChainId ruleChainId, List<RuleNodeId> externalIds) {
        return DaoUtil.convertDataList(ruleNodeRepository.findRuleNodesByRuleChainIdAndExternalIdIn(ruleChainId.getId(),
                externalIds.stream().map(RuleNodeId::getId).collect(Collectors.toList())));
    }

    @Override
    public void deleteByIdIn(List<RuleNodeId> ruleNodeIds) {
        ruleNodeRepository.deleteAllById(ruleNodeIds.stream().map(RuleNodeId::getId).collect(Collectors.toList()));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_NODE;
    }

}
