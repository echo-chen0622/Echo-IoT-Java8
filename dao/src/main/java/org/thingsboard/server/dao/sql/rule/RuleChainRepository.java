package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;

import java.util.List;
import java.util.UUID;

public interface RuleChainRepository extends JpaRepository<RuleChainEntity, UUID>, ExportableEntityRepository<RuleChainEntity> {

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND rc.type = :type " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                @Param("type") RuleChainType type,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                  @Param("edgeId") UUID edgeId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE_AUTO_ASSIGN_RULE_CHAIN' " +
            "AND re.relationType = 'Contains' AND re.fromId = :tenantId AND re.fromType = 'TENANT' " +
            "AND LOWER(rc.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<RuleChainEntity> findAutoAssignByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);


    RuleChainEntity findByTenantIdAndTypeAndRootIsTrue(UUID tenantId, RuleChainType ruleChainType);

    Long countByTenantId(UUID tenantId);

    List<RuleChainEntity> findByTenantIdAndTypeAndName(UUID tenantId, RuleChainType type, String name);

    @Query("SELECT externalId FROM RuleChainEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
