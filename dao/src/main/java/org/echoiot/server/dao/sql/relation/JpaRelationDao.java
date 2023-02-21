package org.echoiot.server.dao.sql.relation;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.RelationCompositeKey;
import org.echoiot.server.dao.model.sql.RelationEntity;
import org.echoiot.server.dao.relation.RelationDao;
import org.echoiot.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    private static final List<String> ALL_TYPE_GROUP_NAMES = new ArrayList<>();

    static {
        Arrays.stream(RelationTypeGroup.values()).map(RelationTypeGroup::name).forEach(ALL_TYPE_GROUP_NAMES::add);
    }

    @Resource
    private RelationRepository relationRepository;

    @Resource
    private RelationInsertRepository relationInsertRepository;

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, @NotNull EntityId from, @NotNull RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, @NotNull EntityId from) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroupIn(
                        from.getId(),
                        from.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    @Override
    public List<EntityRelation> findAllByFromAndType(TenantId tenantId, @NotNull EntityId from, String relationType, @NotNull RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, @NotNull EntityId to, @NotNull RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, @NotNull EntityId to) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroupIn(
                        to.getId(),
                        to.getEntityType().name(),
                        ALL_TYPE_GROUP_NAMES));
    }

    @Override
    public List<EntityRelation> findAllByToAndType(TenantId tenantId, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, @NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        return service.submit(() -> checkRelation(tenantId, from, to, relationType, typeGroup));
    }

    @Override
    public boolean checkRelation(TenantId tenantId, @NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        @NotNull RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return relationRepository.existsById(key);
    }

    @Override
    public EntityRelation getRelation(TenantId tenantId, @NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        @NotNull RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return DaoUtil.getData(relationRepository.findById(key));
    }

    @NotNull
    private RelationCompositeKey getRelationCompositeKey(@NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        return new RelationCompositeKey(from.getId(),
                from.getEntityType().name(),
                to.getId(),
                to.getEntityType().name(),
                relationType,
                typeGroup.name());
    }

    @Override
    public boolean saveRelation(TenantId tenantId, @NotNull EntityRelation relation) {
        return relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null;
    }

    @Override
    public void saveRelations(TenantId tenantId, @NotNull Collection<EntityRelation> relations) {
        @NotNull List<RelationEntity> entities = relations.stream().map(RelationEntity::new).collect(Collectors.toList());
        relationInsertRepository.saveOrUpdate(entities);
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, @NotNull EntityRelation relation) {
        return service.submit(() -> relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null);
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, @NotNull EntityRelation relation) {
        @NotNull RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, @NotNull EntityRelation relation) {
        @NotNull RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, @NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        @NotNull RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, @NotNull EntityId from, @NotNull EntityId to, String relationType, @NotNull RelationTypeGroup typeGroup) {
        @NotNull RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    private boolean deleteRelationIfExists(@NotNull RelationCompositeKey key) {
        boolean relationExistsBeforeDelete = relationRepository.existsById(key);
        if (relationExistsBeforeDelete) {
            try {
                relationRepository.deleteById(key);
            } catch (DataAccessException e) {
                log.debug("[{}] Concurrency exception while deleting relation", key, e);
            }
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public void deleteOutboundRelations(TenantId tenantId, @NotNull EntityId entity) {
        try {
            relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    @Override
    public void deleteInboundRelations(TenantId tenantId, @NotNull EntityId entity) {
        try {
            relationRepository.deleteByToIdAndToTypeAndRelationTypeGroupIn(entity.getId(), entity.getEntityType().name(), ALL_TYPE_GROUP_NAMES);
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, @NotNull EntityId entity) {
        return service.submit(
                () -> {
                    boolean relationExistsBeforeDelete = relationRepository
                            .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                            .size() > 0;
                    if (relationExistsBeforeDelete) {
                        relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
                    }
                    return relationExistsBeforeDelete;
                });
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit) {
        return DaoUtil.convertDataList(relationRepository.findRuleNodeToRuleChainRelations(ruleChainType, PageRequest.of(0, limit)));
    }
}
