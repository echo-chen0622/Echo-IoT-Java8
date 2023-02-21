package org.echoiot.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.RuleChainEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.rule.RuleChainDao;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleChainDao extends JpaAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Resource
    private RuleChainRepository ruleChainRepository;

    @NotNull
    @Override
    protected Class<RuleChainEntity> getEntityClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected JpaRepository<RuleChainEntity, UUID> getRepository() {
        return ruleChainRepository;
    }

    @NotNull
    @Override
    public PageData<RuleChain> findRuleChainsByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, @NotNull PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RuleChain findRootRuleChainByTenantIdAndType(UUID tenantId, RuleChainType type) {
        log.debug("Try to find root rule chain by tenantId [{}] and type [{}]", tenantId, type);
        return DaoUtil.getData(ruleChainRepository.findByTenantIdAndTypeAndRootIsTrue(tenantId, type));
    }

    @NotNull
    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, @NotNull PageLink pageLink) {
        log.debug("Try to find rule chains by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(ruleChainRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        log.debug("Try to find auto assign to edge rule chains by tenantId [{}]", tenantId);
        return DaoUtil.toPageData(ruleChainRepository
                .findAutoAssignByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Collection<RuleChain> findByTenantIdAndTypeAndName(@NotNull TenantId tenantId, RuleChainType type, String name) {
        return DaoUtil.convertDataList(ruleChainRepository.findByTenantIdAndTypeAndName(tenantId.getId(), type, name));
    }

    @Override
    public Long countByTenantId(@NotNull TenantId tenantId) {
        return ruleChainRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public RuleChain findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(ruleChainRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<RuleChain> findByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return findRuleChainsByTenantId(tenantId, pageLink);
    }

    @Nullable
    @Override
    public RuleChainId getExternalIdByInternal(@NotNull RuleChainId internalId) {
        return Optional.ofNullable(ruleChainRepository.getExternalIdById(internalId.getId()))
                .map(RuleChainId::new).orElse(null);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
