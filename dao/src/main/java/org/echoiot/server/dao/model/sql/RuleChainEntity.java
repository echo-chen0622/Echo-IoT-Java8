package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.model.SearchTextEntity;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_CHAIN_COLUMN_FAMILY_NAME)
public class RuleChainEntity extends BaseSqlEntity<RuleChain> implements SearchTextEntity<RuleChain> {

    @Column(name = ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.RULE_CHAIN_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.RULE_CHAIN_TYPE_PROPERTY)
    private RuleChainType type;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY)
    private UUID firstRuleNodeId;

    @Column(name = ModelConstants.RULE_CHAIN_ROOT_PROPERTY)
    private boolean root;

    @Column(name = ModelConstants.DEBUG_MODE)
    private boolean debugMode;

    @Type(type = "json")
    @Column(name = ModelConstants.RULE_CHAIN_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public RuleChainEntity() {
    }

    public RuleChainEntity(@NotNull RuleChain ruleChain) {
        if (ruleChain.getId() != null) {
            this.setUuid(ruleChain.getUuidId());
        }
        this.setCreatedTime(ruleChain.getCreatedTime());
        this.tenantId = DaoUtil.getId(ruleChain.getTenantId());
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        this.searchText = ruleChain.getName();
        if (ruleChain.getFirstRuleNodeId() != null) {
            this.firstRuleNodeId = ruleChain.getFirstRuleNodeId().getId();
        }
        this.root = ruleChain.isRoot();
        this.debugMode = ruleChain.isDebugMode();
        this.configuration = ruleChain.getConfiguration();
        this.additionalInfo = ruleChain.getAdditionalInfo();
        if (ruleChain.getExternalId() != null) {
            this.externalId = ruleChain.getExternalId().getId();
        }
    }

    @Override
    public String getSearchTextSource() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @NotNull
    @Override
    public RuleChain toData() {
        @NotNull RuleChain ruleChain = new RuleChain(new RuleChainId(this.getUuid()));
        ruleChain.setCreatedTime(createdTime);
        ruleChain.setTenantId(TenantId.fromUUID(tenantId));
        ruleChain.setName(name);
        ruleChain.setType(type);
        if (firstRuleNodeId != null) {
            ruleChain.setFirstRuleNodeId(new RuleNodeId(firstRuleNodeId));
        }
        ruleChain.setRoot(root);
        ruleChain.setDebugMode(debugMode);
        ruleChain.setConfiguration(configuration);
        ruleChain.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            ruleChain.setExternalId(new RuleChainId(externalId));
        }
        return ruleChain;
    }
}
