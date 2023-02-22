package org.echoiot.server.dao.rule;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class BaseRuleNodeStateService extends AbstractEntityService implements RuleNodeStateService {

    @Resource
    private RuleNodeStateDao ruleNodeStateDao;

    @Override
    public PageData<RuleNodeState> findByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId, PageLink pageLink) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeId(ruleNodeId.getId(), pageLink);
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("RuleNode id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        return ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    @Override
    public RuleNodeState save(TenantId tenantId, RuleNodeState ruleNodeState) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        return saveOrUpdate(tenantId, ruleNodeState, false);
    }

    @Override
    public void removeByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeId(ruleNodeId.getId());
    }

    @Override
    public void removeByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId) {
        if (tenantId == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (ruleNodeId == null) {
            throw new DataValidationException("Rule node id should be specified!.");
        }
        if (entityId == null) {
            throw new DataValidationException("Entity id should be specified!.");
        }
        ruleNodeStateDao.removeByRuleNodeIdAndEntityId(ruleNodeId.getId(), entityId.getId());
    }

    public RuleNodeState saveOrUpdate(TenantId tenantId, RuleNodeState ruleNodeState, boolean update) {
        try {
            if (update) {
                RuleNodeState old = ruleNodeStateDao.findByRuleNodeIdAndEntityId(ruleNodeState.getRuleNodeId().getId(), ruleNodeState.getEntityId().getId());
                if (old != null && !old.getId().equals(ruleNodeState.getId())) {
                    ruleNodeState.setId(old.getId());
                    ruleNodeState.setCreatedTime(old.getCreatedTime());
                }
            }
            return ruleNodeStateDao.save(tenantId, ruleNodeState);
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("rule_node_state_unq_key")) {
                if (!update) {
                    return saveOrUpdate(tenantId, ruleNodeState, true);
                } else {
                    throw new DataValidationException("Rule node state for such rule node id and entity id already exists!");
                }
            } else {
                throw t;
            }
        }
    }
}
