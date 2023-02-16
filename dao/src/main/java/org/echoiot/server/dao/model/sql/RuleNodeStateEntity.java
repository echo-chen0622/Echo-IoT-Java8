package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.RuleNodeStateId;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_NODE_STATE_TABLE_NAME)
public class RuleNodeStateEntity extends BaseSqlEntity<RuleNodeState> {

    @Column(name = ModelConstants.RULE_NODE_STATE_NODE_ID_PROPERTY)
    private UUID ruleNodeId;

    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_TYPE_PROPERTY)
    private String entityType;

    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = ModelConstants.RULE_NODE_STATE_DATA_PROPERTY)
    private String stateData;

    public RuleNodeStateEntity() {
    }

    public RuleNodeStateEntity(RuleNodeState ruleNodeState) {
        if (ruleNodeState.getId() != null) {
            this.setUuid(ruleNodeState.getUuidId());
        }
        this.setCreatedTime(ruleNodeState.getCreatedTime());
        this.ruleNodeId = DaoUtil.getId(ruleNodeState.getRuleNodeId());
        this.entityId = ruleNodeState.getEntityId().getId();
        this.entityType = ruleNodeState.getEntityId().getEntityType().name();
        this.stateData = ruleNodeState.getStateData();
    }

    @Override
    public RuleNodeState toData() {
        RuleNodeState ruleNode = new RuleNodeState(new RuleNodeStateId(this.getUuid()));
        ruleNode.setCreatedTime(createdTime);
        ruleNode.setRuleNodeId(new RuleNodeId(ruleNodeId));
        ruleNode.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        ruleNode.setStateData(stateData);
        return ruleNode;
    }
}
