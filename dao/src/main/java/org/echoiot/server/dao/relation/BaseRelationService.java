package org.echoiot.server.dao.relation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.*;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.ConstraintValidator;
import org.echoiot.server.dao.sql.JpaExecutorService;
import org.echoiot.server.dao.sql.relation.JpaRelationQueryExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static org.echoiot.server.dao.service.Validator.validateId;

/**
 * Created by Echo on 28.04.17.
 */
@Service
@Slf4j
public class BaseRelationService implements RelationService {

    private final RelationDao relationDao;
    private final EntityService entityService;
    private final TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache;
    private final ApplicationEventPublisher eventPublisher;
    private final JpaExecutorService executor;
    private final JpaRelationQueryExecutorService relationsExecutor;
    protected ScheduledExecutorService timeoutExecutorService;

    @Value("${sql.relations.query_timeout:20}")
    private Integer relationQueryTimeout;

    public BaseRelationService(RelationDao relationDao, @Lazy EntityService entityService,
                               TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache,
                               ApplicationEventPublisher eventPublisher, JpaExecutorService executor,
                               JpaRelationQueryExecutorService relationsExecutor) {
        this.relationDao = relationDao;
        this.entityService = entityService;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
        this.executor = executor;
        this.relationsExecutor = relationsExecutor;
    }

    @PostConstruct
    public void init() {
        timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(EchoiotThreadFactory.forName("relations-query-timeout"));
    }

    @PreDestroy
    public void destroy() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    @TransactionalEventListener(classes = EntityRelationEvent.class)
    public void handleEvictEvent(EntityRelationEvent event) {
        List<RelationCacheKey> keys = new ArrayList<>(5);
        keys.add(new RelationCacheKey(event.getFrom(), event.getTo(), event.getType(), event.getTypeGroup()));
        keys.add(new RelationCacheKey(event.getFrom(), null, event.getType(), event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(event.getFrom(), null, null, event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(null, event.getTo(), event.getType(), event.getTypeGroup(), EntitySearchDirection.TO));
        keys.add(new RelationCacheKey(null, event.getTo(), null, event.getTypeGroup(), EntitySearchDirection.TO));
        cache.evict(keys);
    }

    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelationAsync(tenantId, from, to, relationType, typeGroup);
    }

    @Override
    public boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelation(tenantId, from, to, relationType, typeGroup);
    }

    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        RelationCacheKey cacheKey = new RelationCacheKey(from, to, relationType, typeGroup);
        return cache.getAndPutInTransaction(cacheKey,
                () -> {
                    log.trace("FETCH EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
                    return relationDao.getRelation(tenantId, from, to, relationType, typeGroup);
                },
                RelationCacheValue::getRelation,
                relations -> RelationCacheValue.builder().relation(relations).build(), false);
    }

    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelation [{}]", relation);
        validate(relation);
        var result = relationDao.saveRelation(tenantId, relation);
        publishEvictEvent(EntityRelationEvent.from(relation));
        return result;
    }

    @Override
    public void saveRelations(TenantId tenantId, List<EntityRelation> relations) {
        log.trace("Executing saveRelations [{}]", relations);
        for (EntityRelation relation : relations) {
            validate(relation);
        }
        for (List<EntityRelation> partition : Lists.partition(relations, 1024)) {
            relationDao.saveRelations(tenantId, partition);
        }
        for (EntityRelation relation : relations) {
            publishEvictEvent(EntityRelationEvent.from(relation));
        }
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.saveRelationAsync(tenantId, relation);
        future.addListener(() -> handleEvictEvent(EntityRelationEvent.from(relation)), MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing DeleteRelation [{}]", relation);
        validate(relation);
        var result = relationDao.deleteRelation(tenantId, relation);
        //TODO: evict cache only if the relation was deleted. Note: relationDao.deleteRelation requires improvement.
        publishEvictEvent(EntityRelationEvent.from(relation));
        return result;
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing deleteRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.deleteRelationAsync(tenantId, relation);
        future.addListener(() -> handleEvictEvent(EntityRelationEvent.from(relation)), MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var result = relationDao.deleteRelation(tenantId, from, to, relationType, typeGroup);
        //TODO: evict cache only if the relation was deleted. Note: relationDao.deleteRelation requires improvement.
        publishEvictEvent(new EntityRelationEvent(from, to, relationType, typeGroup));
        return result;
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var future = relationDao.deleteRelationAsync(tenantId, from, to, relationType, typeGroup);
        EntityRelationEvent event = new EntityRelationEvent(from, to, relationType, typeGroup);
        future.addListener(() -> handleEvictEvent(event), MoreExecutors.directExecutor());
        return future;
    }

    @Transactional
    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        validate(entityId);
        List<EntityRelation> inboundRelations = new ArrayList<>(relationDao.findAllByTo(tenantId, entityId));
        List<EntityRelation> outboundRelations = new ArrayList<>(relationDao.findAllByFrom(tenantId, entityId));

        if (!inboundRelations.isEmpty()) {
            try {
                relationDao.deleteInboundRelations(tenantId, entityId);
            } catch (ConcurrencyFailureException e) {
                log.debug("Concurrency exception while deleting relations [{}]", inboundRelations, e);
            }

            for (EntityRelation relation : inboundRelations) {
                eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            }
        }

        if (!outboundRelations.isEmpty()) {
            relationDao.deleteOutboundRelations(tenantId, entityId);

            for (EntityRelation relation : outboundRelations) {
                eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            }
        }
    }

    private List<ListenableFuture<Boolean>> deleteRelationGroupsAsync(TenantId tenantId, List<List<EntityRelation>> relations, boolean deleteFromDb) {
        List<ListenableFuture<Boolean>> results = new ArrayList<>();
        for (List<EntityRelation> relationList : relations) {
            relationList.forEach(relation -> results.add(deleteAsync(tenantId, relation, deleteFromDb)));
        }
        return results;
    }

    private ListenableFuture<Boolean> deleteAsync(TenantId tenantId, EntityRelation relation, boolean deleteFromDb) {
        if (deleteFromDb) {
            return Futures.transform(relationDao.deleteRelationAsync(tenantId, relation),
                    bool -> {
                        handleEvictEvent(EntityRelationEvent.from(relation));
                        return bool;
                    }, MoreExecutors.directExecutor());
        } else {
            handleEvictEvent(EntityRelationEvent.from(relation));
            return Futures.immediateFuture(false);
        }
    }

    @Override
    public List<EntityRelation> findByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        validate(from);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFrom(tenantId, from, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);

        var cacheValue = cache.get(RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build());

        if (cacheValue != null && cacheValue.get() != null) {
            return Futures.immediateFuture(cacheValue.get().getRelations());
        } else {
            //Disabled cache put for the async requests due to limitations of the cache implementation (Redis lib does not support thread-safe transactions)
            return executor.submit(() -> findByFrom(tenantId, from, typeGroup));
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = executor.submit(() -> relationDao.findAllByFrom(tenantId, from, typeGroup));
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getTo,
                                    EntityRelationInfo::setToName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    @Override
    public List<EntityRelation> findByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFromAndType(tenantId, from, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFromAndType [{}][{}][{}]", from, relationType, typeGroup);
        validate(from);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByFromAndType(tenantId, from, relationType, typeGroup));
    }

    @Override
    public List<EntityRelation> findByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        validate(to);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByTo(tenantId, to, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAsync [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByTo(tenantId, to, typeGroup));
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByTo [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = findByToAsync(tenantId, to, typeGroup);
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getFrom,
                                    EntityRelationInfo::setFromName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<EntityRelationInfo> fetchRelationInfoAsync(TenantId tenantId, EntityRelation relation,
                                                                        Function<EntityRelation, EntityId> entityIdGetter,
                                                                        BiConsumer<EntityRelationInfo, String> entityNameSetter) {
        ListenableFuture<String> entityName = entityService.fetchEntityNameAsync(tenantId, entityIdGetter.apply(relation));
        return Futures.transform(entityName, entityName1 -> {
            EntityRelationInfo entityRelationInfo1 = new EntityRelationInfo(relation);
            entityNameSetter.accept(entityRelationInfo1, entityName1);
            return entityRelationInfo1;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public List<EntityRelation> findByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndType [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByToAndType(tenantId, to, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndTypeAsync [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByToAndType(tenantId, to, relationType, typeGroup));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findByQuery [{}]", query);
        RelationsSearchParameters params = query.getParameters();
        final List<RelationEntityTypeFilter> filters = query.getFilters();
        if (filters == null || filters.isEmpty()) {
            log.debug("Filters are not set [{}]", query);
        }

        int maxLvl = params.getMaxLevel() > 0 ? params.getMaxLevel() : Integer.MAX_VALUE;

        try {
            ListenableFuture<Set<EntityRelation>> relationSet = findRelationsRecursively(tenantId, params.getEntityId(), params.getDirection(),
                    params.getRelationTypeGroup(), maxLvl, params.isFetchLastLevelOnly(), new ConcurrentHashMap<>());
            return Futures.transform(relationSet, input -> {
                List<EntityRelation> relations = new ArrayList<>();
                if (filters == null || filters.isEmpty()) {
                    relations.addAll(input);
                    return relations;
                }
                for (EntityRelation relation : input) {
                    if (matchFilters(filters, relation, params.getDirection())) {
                        relations.add(relation);
                    }
                }
                return relations;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.warn("Failed to query relations: [{}]", query, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findInfoByQuery [{}]", query);
        ListenableFuture<List<EntityRelation>> relations = findByQuery(tenantId, query);
        EntitySearchDirection direction = query.getParameters().getDirection();
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    relation2 -> direction == EntitySearchDirection.FROM ? relation2.getTo() : relation2.getFrom(),
                                    (EntityRelationInfo relationInfo, String entityName) -> {
                                        if (direction == EntitySearchDirection.FROM) {
                                            relationInfo.setToName(entityName);
                                        } else {
                                            relationInfo.setFromName(entityName);
                                        }
                                    }))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    @Override
    public void removeRelations(TenantId tenantId, EntityId entityId) {
        log.trace("removeRelations {}", entityId);

        List<EntityRelation> relations = new ArrayList<>();
        for (RelationTypeGroup relationTypeGroup : RelationTypeGroup.values()) {
            relations.addAll(findByFrom(tenantId, entityId, relationTypeGroup));
            relations.addAll(findByTo(tenantId, entityId, relationTypeGroup));
        }

        for (EntityRelation relation : relations) {
            deleteRelation(tenantId, relation);
        }
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(TenantId tenantId, RuleChainType ruleChainType, int limit) {
        log.trace("Executing findRuleNodeToRuleChainRelations, tenantId [{}], ruleChainType {} and limit {}", tenantId, ruleChainType, limit);
        validateId(tenantId, "Invalid tenant id: " + tenantId);
        return relationDao.findRuleNodeToRuleChainRelations(ruleChainType, limit);
    }

    protected void validate(EntityRelation relation) {
        if (relation == null) {
            throw new DataValidationException("Relation type should be specified!");
        }
        ConstraintValidator.validateFields(relation);
        validate(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }

    protected void validate(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        validateType(type);
        validateTypeGroup(typeGroup);
        if (from == null) {
            throw new DataValidationException("Relation should contain from entity!");
        }
        if (to == null) {
            throw new DataValidationException("Relation should contain to entity!");
        }
    }

    private void validateType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new DataValidationException("Relation type should be specified!");
        }
    }

    private void validateTypeGroup(RelationTypeGroup typeGroup) {
        if (typeGroup == null) {
            throw new DataValidationException("Relation type group should be specified!");
        }
    }

    protected void validate(EntityId entity) {
        if (entity == null) {
            throw new DataValidationException("Entity should be specified!");
        }
    }

    private boolean matchFilters(List<RelationEntityTypeFilter> filters, EntityRelation relation, EntitySearchDirection direction) {
        for (RelationEntityTypeFilter filter : filters) {
            if (match(filter, relation, direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(RelationEntityTypeFilter filter, EntityRelation relation, EntitySearchDirection direction) {
        if (StringUtils.isEmpty(filter.getRelationType()) || filter.getRelationType().equals(relation.getType())) {
            if (filter.getEntityTypes() == null || filter.getEntityTypes().isEmpty()) {
                return true;
            } else {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                return filter.getEntityTypes().contains(entityId.getEntityType());
            }
        } else {
            return false;
        }
    }

    @RequiredArgsConstructor
    private static class RelationQueueCtx {
        final SettableFuture<Set<EntityRelation>> future = SettableFuture.create();
        final Set<EntityRelation> result = ConcurrentHashMap.newKeySet();
        final Queue<RelationTask> tasks = new ConcurrentLinkedQueue<>();

        final TenantId tenantId;
        final EntitySearchDirection direction;
        final RelationTypeGroup relationTypeGroup;
        final boolean fetchLastLevelOnly;
        final int maxLvl;
        final ConcurrentHashMap<EntityId, Boolean> uniqueMap;

    }

    @RequiredArgsConstructor
    private static class RelationTask {
        private final int currentLvl;
        private final EntityId root;
        private final List<EntityRelation> prevRelations;
    }

    private void processQueue(RelationQueueCtx ctx) {
        RelationTask task = ctx.tasks.poll();
        while (task != null) {
            List<EntityRelation> relations = findRelations(ctx.tenantId, task.root, ctx.direction, ctx.relationTypeGroup);
            Map<EntityId, List<EntityRelation>> newChildrenRelations = new HashMap<>();
            for (EntityRelation childRelation : relations) {
                log.trace("Found Relation: {}", childRelation);
                EntityId childId = ctx.direction == EntitySearchDirection.FROM ? childRelation.getTo() : childRelation.getFrom();
                if (ctx.uniqueMap.putIfAbsent(childId, Boolean.TRUE) == null) {
                    log.trace("Adding Relation: {}", childId);
                    newChildrenRelations.put(childId, new ArrayList<>());
                }
                if (ctx.fetchLastLevelOnly) {
                    var list = newChildrenRelations.get(childId);
                    if (list != null) {
                        list.add(childRelation);
                    }
                }
            }
            if (ctx.fetchLastLevelOnly) {
                if (relations.isEmpty()) {
                    ctx.result.addAll(task.prevRelations);
                } else if (task.currentLvl == ctx.maxLvl) {
                    ctx.result.addAll(relations);
                }
            } else {
                ctx.result.addAll(relations);
            }
            var finalTask = task;
            newChildrenRelations.forEach((child, childRelations) -> {
                var newLvl = finalTask.currentLvl + 1;
                if (newLvl <= ctx.maxLvl)
                    ctx.tasks.add(new RelationTask(newLvl, child, childRelations));
            });
            task = ctx.tasks.poll();
        }
        ctx.future.set(ctx.result);
    }

    private ListenableFuture<Set<EntityRelation>> findRelationsRecursively(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction,
                                                                           RelationTypeGroup relationTypeGroup, int lvl, boolean fetchLastLevelOnly,
                                                                           final ConcurrentHashMap<EntityId, Boolean> uniqueMap) {
        if (lvl == 0) {
            return Futures.immediateFuture(Collections.emptySet());
        }
        var relationQueueCtx = new RelationQueueCtx(tenantId, direction, relationTypeGroup, fetchLastLevelOnly, lvl, uniqueMap);
        relationQueueCtx.tasks.add(new RelationTask(1, rootId, Collections.emptyList()));
        relationsExecutor.submit(() -> processQueue(relationQueueCtx));
        return Futures.withTimeout(relationQueueCtx.future, relationQueryTimeout, TimeUnit.SECONDS, timeoutExecutorService);
    }


    private List<EntityRelation> findRelations(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction, RelationTypeGroup relationTypeGroup) {
        List<EntityRelation> relations;
        if (relationTypeGroup == null) {
            relationTypeGroup = RelationTypeGroup.COMMON;
        }
        if (direction == EntitySearchDirection.FROM) {
            relations = findByFrom(tenantId, rootId, relationTypeGroup);
        } else {
            relations = findByTo(tenantId, rootId, relationTypeGroup);
        }
        return relations;
    }

    private void publishEvictEvent(EntityRelationEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

}