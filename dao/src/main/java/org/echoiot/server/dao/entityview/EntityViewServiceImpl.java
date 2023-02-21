package org.echoiot.server.dao.entityview;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.entityview.EntityViewSearchQuery;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.sql.JpaExecutorService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.service.Validator.*;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@Service
@Slf4j
public class EntityViewServiceImpl extends AbstractCachedEntityService<EntityViewCacheKey, EntityViewCacheValue, EntityViewEvictEvent> implements EntityViewService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ENTITY_VIEW_ID = "Incorrect entityViewId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    @Resource
    private EntityViewDao entityViewDao;

    @Resource
    private DataValidator<EntityView> entityViewValidator;

    @Resource
    protected JpaExecutorService service;

    @TransactionalEventListener(classes = EntityViewEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull EntityViewEvictEvent event) {
        @NotNull List<EntityViewCacheKey> keys = new ArrayList<>(5);
        keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getNewName()));
        keys.add(EntityViewCacheKey.byId(event.getId()));
        keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getNewEntityId()));
        if (event.getOldEntityId() != null && !event.getOldEntityId().equals(event.getNewEntityId())) {
            keys.add(EntityViewCacheKey.byEntityId(event.getTenantId(), event.getOldEntityId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(EntityViewCacheKey.byName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @NotNull
    @Override
    public EntityView saveEntityView(@NotNull EntityView entityView) {
        log.trace("Executing save entity view [{}]", entityView);
        @org.jetbrains.annotations.Nullable EntityView old = entityViewValidator.validate(entityView, EntityView::getTenantId);
        try {
            EntityView saved = entityViewDao.save(entityView.getTenantId(), entityView);
            publishEvictEvent(new EntityViewEvictEvent(saved.getTenantId(), saved.getId(), saved.getEntityId(), old != null ? old.getEntityId() : null, saved.getName(), old != null ? old.getName() : null));
            return saved;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "entity_view_external_id_unq_key", "Entity View with such external id already exists!");
            throw t;
        }
    }

    @Override
    public EntityView assignEntityViewToCustomer(TenantId tenantId, @NotNull EntityViewId entityViewId, CustomerId customerId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        entityView.setCustomerId(customerId);
        return saveEntityView(entityView);
    }

    @Override
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, @NotNull EntityViewId entityViewId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        entityView.setCustomerId(null);
        return saveEntityView(entityView);
    }

    @Override
    public void unassignCustomerEntityViews(@NotNull TenantId tenantId, @NotNull CustomerId customerId) {
        log.trace("Executing unassignCustomerEntityViews, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEntityViewsUnAssigner.removeEntities(tenantId, customerId);
    }

    @Override
    public EntityViewInfo findEntityViewInfoById(TenantId tenantId, @NotNull EntityViewId entityViewId) {
        log.trace("Executing findEntityViewInfoById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findEntityViewInfoById(tenantId, entityViewId.getId());
    }

    @Override
    public EntityView findEntityViewById(TenantId tenantId, @NotNull EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byId(entityViewId),
                () -> entityViewDao.findById(tenantId, entityViewId.getId())
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);
    }

    @Override
    public EntityView findEntityViewByTenantIdAndName(@NotNull TenantId tenantId, String name) {
        log.trace("Executing findEntityViewByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(EntityViewCacheKey.byName(tenantId, name),
                () -> entityViewDao.findEntityViewByTenantIdAndName(tenantId.getId(), name).orElse(null)
                , EntityViewCacheValue::getEntityView, v -> new EntityViewCacheValue(v, null), true);

    }

    @Override
    public PageData<EntityView> findEntityViewByTenantId(@NotNull TenantId tenantId, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantId(@NotNull TenantId tenantId, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewByTenantIdAndType(@NotNull TenantId tenantId, @NotNull PageLink pageLink, @NotNull String type) {
        log.trace("Executing findEntityViewByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(@NotNull TenantId tenantId, @NotNull String type, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId,
                                                                       @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}]," +
                " pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewInfosByTenantIdAndCustomerId(tenantId.getId(),
                customerId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull PageLink pageLink, @NotNull String type) {
        log.trace("Executing findEntityViewsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}]," +
                " pageLink [{}], type [{}]", tenantId, customerId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId.getId(),
                customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull String type, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}]," +
                " pageLink [{}], type [{}]", tenantId, customerId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        validateString(type, "Incorrect type " + type);
        return entityViewDao.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId.getId(),
                customerId.getId(), type, pageLink);
    }

    @NotNull
    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, @NotNull EntityViewSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        @NotNull ListenableFuture<List<EntityView>> entityViews = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            @NotNull List<ListenableFuture<EntityView>> futures = new ArrayList<>();
            for (@NotNull EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ENTITY_VIEW) {
                    futures.add(findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        entityViews = Futures.transform(entityViews, new Function<List<EntityView>, List<EntityView>>() {
            @Nullable
            @Override
            public List<EntityView> apply(@Nullable List<EntityView> entityViewList) {
                return entityViewList == null ? Collections.emptyList() : entityViewList.stream().filter(entityView -> query.getEntityViewTypes().contains(entityView.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return entityViews;
    }

    @Override
    public ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, @NotNull EntityViewId entityViewId) {
        log.trace("Executing findEntityViewById [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        return entityViewDao.findByIdAsync(tenantId, entityViewId.getId());
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(@NotNull TenantId tenantId, @NotNull EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityIdAsync, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        return service.submit(() -> cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndEntityId(@NotNull TenantId tenantId, @NotNull EntityId entityId) {
        log.trace("Executing findEntityViewsByTenantIdAndEntityId, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityId.getId(), "Incorrect entityId" + entityId);

        return cache.getAndPutInTransaction(EntityViewCacheKey.byEntityId(tenantId, entityId),
                () -> entityViewDao.findEntityViewsByTenantIdAndEntityId(tenantId.getId(), entityId.getId()),
                EntityViewCacheValue::getEntityViews, v -> new EntityViewCacheValue(null, v), true);
    }

    @Override
    @Transactional
    public void deleteEntityView(TenantId tenantId, @NotNull EntityViewId entityViewId) {
        log.trace("Executing deleteEntityView [{}]", entityViewId);
        validateId(entityViewId, INCORRECT_ENTITY_VIEW_ID + entityViewId);
        deleteEntityRelations(tenantId, entityViewId);
        EntityView entityView = entityViewDao.findById(tenantId, entityViewId.getId());
        entityViewDao.removeById(tenantId, entityViewId.getId());
        publishEvictEvent(new EntityViewEvictEvent(entityView.getTenantId(), entityView.getId(), entityView.getEntityId(), null, entityView.getName(), null));
    }

    @Override
    public void deleteEntityViewsByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing deleteEntityViewsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEntityViewRemover.removeEntities(tenantId, tenantId);
    }

    @NotNull
    @Override
    public ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findEntityViewTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEntityViewTypes = entityViewDao.findTenantEntityViewTypesAsync(tenantId.getId());
        return Futures.transform(tenantEntityViewTypes,
                entityViewTypes -> {
                    entityViewTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return entityViewTypes;
                }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public EntityView assignEntityViewToEdge(TenantId tenantId, @NotNull EntityViewId entityViewId, EdgeId edgeId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign entityView to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(entityView.getTenantId().getId())) {
            throw new DataValidationException("Can't assign entityView to edge from different tenant!");
        }

        @NotNull Boolean relationExists = relationService.checkRelation(tenantId, edgeId, entityView.getEntityId(),
                                                                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        if (!relationExists) {
            throw new DataValidationException("Can't assign entity view to edge because related device/asset doesn't assigned to edge!");
        }

        try {
            createRelation(tenantId, new EntityRelation(edgeId, entityViewId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create entityView relation. Edge Id: [{}]", entityViewId, edgeId);
            throw new RuntimeException(e);
        }
        return entityView;
    }

    @Override
    public EntityView unassignEntityViewFromEdge(TenantId tenantId, @NotNull EntityViewId entityViewId, EdgeId edgeId) {
        EntityView entityView = findEntityViewById(tenantId, entityViewId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign entityView from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, entityViewId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete entityView relation. Edge Id: [{}]", entityViewId, edgeId);
            throw new RuntimeException(e);
        }
        return entityView;
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, @NotNull String type, @NotNull PageLink pageLink) {
        log.trace("Executing findEntityViewsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}], pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return entityViewDao.findEntityViewsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private final PaginatedRemover<TenantId, EntityView> tenantEntityViewRemover = new PaginatedRemover<TenantId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull EntityView entity) {
            deleteEntityView(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };

    private final PaginatedRemover<CustomerId, EntityView> customerEntityViewsUnAssigner = new PaginatedRemover<CustomerId, EntityView>() {
        @Override
        protected PageData<EntityView> findEntities(@NotNull TenantId tenantId, @NotNull CustomerId id, PageLink pageLink) {
            return entityViewDao.findEntityViewsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull EntityView entity) {
            unassignEntityViewFromCustomer(tenantId, new EntityViewId(entity.getUuidId()));
        }
    };
}
