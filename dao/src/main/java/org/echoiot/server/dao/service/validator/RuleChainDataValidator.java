package org.echoiot.server.dao.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.*;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.util.ReflectionUtils;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.rule.RuleChainDao;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.ConstraintValidator;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
@Slf4j
public class RuleChainDataValidator extends DataValidator<RuleChain> {

    @Resource
    private RuleChainDao ruleChainDao;

    @Resource
    @Lazy
    private RuleChainService ruleChainService;

    @Resource
    private TenantService tenantService;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, RuleChain data) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxRuleChains = profileConfiguration.getMaxRuleChains();
        validateNumberOfEntitiesPerTenant(tenantId, ruleChainDao, maxRuleChains, EntityType.RULE_CHAIN);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull RuleChain ruleChain) {
        if (StringUtils.isEmpty(ruleChain.getName())) {
            throw new DataValidationException("Rule chain name should be specified!");
        }
        if (ruleChain.getType() == null) {
            ruleChain.setType(RuleChainType.CORE);
        }
        if (ruleChain.getTenantId() == null || ruleChain.getTenantId().isNullUid()) {
            throw new DataValidationException("Rule chain should be assigned to tenant!");
        }
        if (!tenantService.tenantExists(ruleChain.getTenantId())) {
            throw new DataValidationException("Rule chain is referencing to non-existent tenant!");
        }
        if (ruleChain.isRoot() && RuleChainType.CORE.equals(ruleChain.getType())) {
            RuleChain rootRuleChain = ruleChainService.getRootTenantRuleChain(ruleChain.getTenantId());
            if (rootRuleChain != null && !rootRuleChain.getId().equals(ruleChain.getId())) {
                throw new DataValidationException("Another root rule chain is present in scope of current tenant!");
            }
        }
        if (ruleChain.isRoot() && RuleChainType.EDGE.equals(ruleChain.getType())) {
            RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
            if (edgeTemplateRootRuleChain != null && !edgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
                throw new DataValidationException("Another edge template root rule chain is present in scope of current tenant!");
            }
        }
    }

    public static void validateMetaData(@NotNull RuleChainMetaData ruleChainMetaData) {
        ConstraintValidator.validateFields(ruleChainMetaData);
        ruleChainMetaData.getNodes().forEach(RuleChainDataValidator::validateRuleNode);
        if (CollectionUtils.isNotEmpty(ruleChainMetaData.getConnections())) {
            validateCircles(ruleChainMetaData.getConnections());
        }
    }

    public static void validateRuleNode(@NotNull RuleNode ruleNode) {
        @NotNull String errorPrefix = "'" + ruleNode.getName() + "' node configuration is invalid: ";
        ConstraintValidator.validateFields(ruleNode, errorPrefix);
        Object nodeConfig;
        try {
            Class<Object> nodeConfigType = ReflectionUtils.getAnnotationProperty(ruleNode.getType(),
                                                                                 "org.echoiot.rule.engine.api.RuleNode", "configClazz");
            nodeConfig = JacksonUtil.treeToValue(ruleNode.getConfiguration(), nodeConfigType);
        } catch (Exception e) {
            log.warn("Failed to validate node configuration: {}", ExceptionUtils.getRootCauseMessage(e));
            return;
        }
        ConstraintValidator.validateFields(nodeConfig, errorPrefix);
    }

    private static void validateCircles(@NotNull List<NodeConnectionInfo> connectionInfos) {
        @NotNull Map<Integer, Set<Integer>> connectionsMap = new HashMap<>();
        for (@NotNull NodeConnectionInfo nodeConnection : connectionInfos) {
            if (nodeConnection.getFromIndex() == nodeConnection.getToIndex()) {
                throw new DataValidationException("Can't create the relation to yourself.");
            }
            connectionsMap
                    .computeIfAbsent(nodeConnection.getFromIndex(), from -> new HashSet<>())
                    .add(nodeConnection.getToIndex());
        }
        connectionsMap.keySet().forEach(key -> validateCircles(key, connectionsMap.get(key), connectionsMap));
    }

    private static void validateCircles(int from, @Nullable Set<Integer> toList, @NotNull Map<Integer, Set<Integer>> connectionsMap) {
        if (toList == null) {
            return;
        }
        for (Integer to : toList) {
            if (from == to) {
                throw new DataValidationException("Can't create circling relations in rule chain.");
            }
            validateCircles(from, connectionsMap.get(to), connectionsMap);
        }
    }

}
