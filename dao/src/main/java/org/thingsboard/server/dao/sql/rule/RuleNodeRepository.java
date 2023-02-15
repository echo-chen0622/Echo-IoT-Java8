package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;

import java.util.List;
import java.util.UUID;

public interface RuleNodeRepository extends JpaRepository<RuleNodeEntity, UUID> {

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.ruleChainId in " +
            "(select id from RuleChainEntity rc WHERE rc.tenantId = :tenantId) " +
            "AND r.type = :ruleType AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    List<RuleNodeEntity> findRuleNodesByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                        @Param("ruleType") String ruleType,
                                                        @Param("searchText") String searchText);

    @Query("SELECT r FROM RuleNodeEntity r WHERE r.type = :ruleType AND LOWER(r.configuration) LIKE LOWER(CONCAT('%', :searchText, '%')) ")
    Page<RuleNodeEntity> findAllRuleNodesByType(@Param("ruleType") String ruleType,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    List<RuleNodeEntity> findRuleNodesByRuleChainIdAndExternalIdIn(UUID ruleChainId, List<UUID> externalIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM RuleNodeEntity e where e.id in :ids")
    void deleteByIdIn(@Param("ids") List<UUID> ids);

}
