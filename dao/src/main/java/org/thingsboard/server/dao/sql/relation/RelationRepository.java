package org.thingsboard.server.dao.sql.relation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;
import java.util.UUID;

public interface RelationRepository
        extends JpaRepository<RelationEntity, RelationCompositeKey>, JpaSpecificationExecutor<RelationEntity> {

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroup(UUID fromId,
                                                                        String fromType,
                                                                        String relationTypeGroup);

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroupIn(UUID fromId,
                                                                          String fromType,
                                                                          List<String> relationTypeGroups);

    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(UUID fromId,
                                                                                       String fromType,
                                                                                       String relationType,
                                                                                       String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroup(UUID toId,
                                                                    String toType,
                                                                    String relationTypeGroup);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroupIn(UUID toId,
                                                                      String toType,
                                                                      List<String> relationTypeGroups);

    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(UUID toId,
                                                                                   String toType,
                                                                                   String relationType,
                                                                                   String relationTypeGroup);

    List<RelationEntity> findAllByFromIdAndFromType(UUID fromId,
                                                    String fromType);

    @Query("SELECT r FROM RelationEntity r WHERE " +
            "r.relationTypeGroup = 'RULE_NODE' AND r.toType = 'RULE_CHAIN' " +
            "AND r.toId in (SELECT id from RuleChainEntity where type = :ruleChainType )")
    List<RelationEntity> findRuleNodeToRuleChainRelations(@Param("ruleChainType") RuleChainType ruleChainType, Pageable page);

    @Transactional
    <S extends RelationEntity> S save(S entity);

    @Transactional
    void deleteById(RelationCompositeKey id);

    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.fromId = :fromId and r.fromType = :fromType")
    void deleteByFromIdAndFromType(@Param("fromId") UUID fromId, @Param("fromType") String fromType);

    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.toId = :toId and r.toType = :toType and r.relationTypeGroup in :relationTypeGroups")
    void deleteByToIdAndToTypeAndRelationTypeGroupIn(@Param("toId") UUID toId, @Param("toType") String toType, @Param("relationTypeGroups") List<String> relationTypeGroups);

}
