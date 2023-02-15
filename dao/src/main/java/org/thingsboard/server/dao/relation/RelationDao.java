package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.Collection;
import java.util.List;

/**
 * Created by ashvayka on 25.04.17.
 */
public interface RelationDao {

    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from);

    List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to);

    List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean saveRelation(TenantId tenantId, EntityRelation relation);

    void saveRelations(TenantId tenantId, Collection<EntityRelation> relations);

    ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation);

    boolean deleteRelation(TenantId tenantId, EntityRelation relation);

    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation);

    boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    void deleteOutboundRelations(TenantId tenantId, EntityId entity);

    void deleteInboundRelations(TenantId tenantId, EntityId entity);

    ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity);

    List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit);
}
