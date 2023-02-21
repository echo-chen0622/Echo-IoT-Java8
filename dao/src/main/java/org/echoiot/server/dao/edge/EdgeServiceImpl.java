package org.echoiot.server.dao.edge;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeInfo;
import org.echoiot.server.common.data.edge.EdgeSearchQuery;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.IdBased;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.user.UserService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.DaoUtil.toUUIDs;
import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class EdgeServiceImpl extends AbstractCachedEntityService<EdgeCacheKey, Edge, EdgeCacheEvictEvent> implements EdgeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Resource
    private EdgeDao edgeDao;

    @Resource
    private UserService userService;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private RelationService relationService;

    @Resource
    private DataValidator<Edge> edgeValidator;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    @TransactionalEventListener(classes = EdgeCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull EdgeCacheEvictEvent event) {
        @NotNull List<EdgeCacheKey> keys = new ArrayList<>(2);
        keys.add(new EdgeCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new EdgeCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public Edge findEdgeById(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findById(tenantId, edgeId.getId());
    }

    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("Executing findEdgeInfoById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findEdgeInfoById(tenantId, edgeId.getId());
    }

    @Override
    public ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findByIdAsync(tenantId, edgeId.getId());
    }

    @Override
    public Edge findEdgeByTenantIdAndName(@NotNull TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new EdgeCacheKey(tenantId, name),
                () -> edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    @Override
    public Optional<Edge> findEdgeByRoutingKey(@NotNull TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public Edge saveEdge(@NotNull Edge edge) {
        log.trace("Executing saveEdge [{}]", edge);
        Edge oldEdge = edgeValidator.validate(edge, Edge::getTenantId);
        @NotNull EdgeCacheEvictEvent evictEvent = new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), oldEdge != null ? oldEdge.getName() : null);
        try {
            var savedEdge = edgeDao.save(edge.getTenantId(), edge);
            publishEvictEvent(evictEvent);
            return savedEdge;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            @org.jetbrains.annotations.Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && e.getConstraintName().equalsIgnoreCase("edge_name_unq_key")) {
                throw new DataValidationException("Edge with such name already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, @NotNull EdgeId edgeId, CustomerId customerId) {
        log.trace("[{}] Executing assignEdgeToCustomer [{}][{}]", tenantId, edgeId, customerId);
        Edge edge = findEdgeById(tenantId, edgeId);
        edge.setCustomerId(customerId);
        return saveEdge(edge);
    }

    @Override
    public Edge unassignEdgeFromCustomer(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("[{}] Executing unassignEdgeFromCustomer [{}]", tenantId, edgeId);
        Edge edge = findEdgeById(tenantId, edgeId);
        edge.setCustomerId(null);
        return saveEdge(edge);
    }

    @Override
    @Transactional
    public void deleteEdge(TenantId tenantId, @NotNull EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());

        deleteEntityRelations(tenantId, edgeId);

        edgeDao.removeById(tenantId, edgeId.getId());

        publishEvictEvent(new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), null));
    }

    @Override
    public PageData<Edge> findEdgesByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdAndIdsAsync, tenantId [{}], edgeIds [{}]", tenantId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void deleteEdgesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEdgesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], edgeIds [{}]", tenantId, customerId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(edgeIds));
    }

    @Override
    public void unassignCustomerEdges(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEdges, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEdgeUnassigner.removeEntities(tenantId, customerId);
    }

    @NotNull
    @Override
    public ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, @NotNull EdgeSearchQuery query) {
        log.trace("[{}] Executing findEdgesByQuery [{}]", tenantId, query);
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        @NotNull ListenableFuture<List<Edge>> edges = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            @NotNull List<ListenableFuture<Edge>> futures = new ArrayList<>();
            for (@NotNull EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.EDGE) {
                    futures.add(findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        edges = Futures.transform(edges, new Function<List<Edge>, List<Edge>>() {
            @Nullable
            @Override
            public List<Edge> apply(@Nullable List<Edge> edgeList) {
                return edgeList == null ?
                        Collections.emptyList() :
                        edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return edges;
    }

    @NotNull
    @Override
    public ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findEdgeTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEdgeTypes = edgeDao.findTenantEdgeTypesAsync(tenantId.getId());
        return Futures.transform(tenantEdgeTypes,
                edgeTypes -> {
                    edgeTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return edgeTypes;
                }, MoreExecutors.directExecutor());
    }

    @Override
    public void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignDefaultRuleChainsToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, pageLink);
            if (pageData.getData().size() > 0) {
                for (@NotNull RuleChain ruleChain : pageData.getData()) {
                    ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), edgeId);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(@NotNull TenantId tenantId, @NotNull EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    private final PaginatedRemover<TenantId, Edge> tenantEdgesRemover =
            new PaginatedRemover<TenantId, Edge>() {

                @Override
                protected PageData<Edge> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return edgeDao.findEdgesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull Edge entity) {
                    deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
                }
            };

    private final PaginatedRemover<CustomerId, Edge> customerEdgeUnassigner = new PaginatedRemover<CustomerId, Edge>() {

        @Override
        protected PageData<Edge> findEntities(@NotNull TenantId tenantId, @NotNull CustomerId id, PageLink pageLink) {
            return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull Edge entity) {
            unassignEdgeFromCustomer(tenantId, new EdgeId(entity.getUuidId()));
        }
    };

    @org.jetbrains.annotations.Nullable
    @Override
    public List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, @NotNull EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        @NotNull PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        @NotNull List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    @NotNull
    @Override
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(@NotNull TenantId tenantId, @NotNull EntityId entityId, PageLink pageLink) {
        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}] [{}]", tenantId, entityId, pageLink);
        switch (entityId.getEntityType()) {
            case TENANT:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case OTA_PACKAGE:
                return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
            case CUSTOMER:
                return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink));
            case EDGE:
                @NotNull List<EdgeId> edgeIds = Collections.singletonList(new EdgeId(entityId.getId()));
                return new PageData<>(edgeIds, 1, 1, false);
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case RULE_CHAIN:
                return convertToEdgeIds(findEdgesByTenantIdAndEntityId(tenantId, entityId, pageLink));
            case USER:
                User userById = userService.findUserById(tenantId, new UserId(entityId.getId()));
                if (userById == null) {
                    return createEmptyEdgeIdPageData();
                }
                if (userById.getCustomerId() == null || userById.getCustomerId().isNullUid()) {
                    return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
                } else {
                    return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, userById.getCustomerId(), pageLink));
                }
            default:
                log.warn("[{}] Unsupported entity type {}", tenantId, entityId.getEntityType());
                return createEmptyEdgeIdPageData();
        }
    }

    @NotNull
    private PageData<EdgeId> createEmptyEdgeIdPageData() {
        return new PageData<>(new ArrayList<>(), 0, 0, false);
    }

    @NotNull
    private PageData<EdgeId> convertToEdgeIds(@org.jetbrains.annotations.Nullable PageData<Edge> pageData) {
        if (pageData == null) {
            return createEmptyEdgeIdPageData();
        }
        @NotNull List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName) {
        @NotNull List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        @NotNull List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).collect(Collectors.toList());
        ObjectNode result = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        for (@NotNull RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleNode> ruleNodes =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getNodes();
            if (ruleNodes != null && !ruleNodes.isEmpty()) {
                @NotNull List<RuleChainId> connectedRuleChains =
                        ruleNodes.stream()
                                .filter(rn -> rn.getType().equals(tbRuleChainInputNodeClassName))
                                .map(rn -> new RuleChainId(UUID.fromString(rn.getConfiguration().get("ruleChainId").asText())))
                                .collect(Collectors.toList());
                @NotNull List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = JacksonUtil.OBJECT_MAPPER.createArrayNode();
                    for (String missingRuleChain : missingRuleChains) {
                        array.add(missingRuleChain);
                    }
                    result.set(edgeRuleChain.getName(), array);
                }
            }
        }
        return result.toString();
    }

    @NotNull
    private List<RuleChain> findEdgeRuleChains(TenantId tenantId, EdgeId edgeId) {
        @NotNull List<RuleChain> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return result;
    }
}
