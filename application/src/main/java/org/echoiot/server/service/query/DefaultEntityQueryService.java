package org.echoiot.server.service.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.common.util.KvUtil;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.security.AccessValidator;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.subscription.TbAttributeSubscriptionScope;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@TbCoreComponent
public class DefaultEntityQueryService implements EntityQueryService {

    @Resource
    private EntityService entityService;

    @Resource
    private AlarmService alarmService;

    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;

    @Resource
    private DbCallbackExecutorService dbCallbackExecutor;

    @Resource
    private TimeseriesService timeseriesService;

    @Resource
    private AttributesService attributesService;

    @Override
    public long countEntitiesByQuery(@NotNull SecurityUser securityUser, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(@NotNull SecurityUser securityUser, @NotNull EntityDataQuery query) {
        if (query.getKeyFilters() != null) {
            resolveDynamicValuesInPredicates(
                    query.getKeyFilters().stream()
                            .map(KeyFilter::getPredicate)
                            .collect(Collectors.toList()),
                    securityUser
            );
        }
        return entityService.findEntityDataByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    private void resolveDynamicValuesInPredicates(@NotNull List<KeyFilterPredicate> predicates, @NotNull SecurityUser user) {
        predicates.forEach(predicate -> {
            if (predicate.getType() == FilterPredicateType.COMPLEX) {
                resolveDynamicValuesInPredicates(
                        ((ComplexFilterPredicate) predicate).getPredicates(),
                        user
                );
            } else {
                setResolvedValue(user, (SimpleKeyFilterPredicate<?>) predicate);
            }
        });
    }

    private void setResolvedValue(@NotNull SecurityUser user, @NotNull SimpleKeyFilterPredicate<?> predicate) {
        DynamicValue<?> dynamicValue = predicate.getValue().getDynamicValue();
        if (dynamicValue != null && dynamicValue.getResolvedValue() == null) {
            resolveDynamicValue(dynamicValue, user, predicate.getType());
        }
    }

    private <T> void resolveDynamicValue(@NotNull DynamicValue<T> dynamicValue, @NotNull SecurityUser user, @NotNull FilterPredicateType predicateType) {
        EntityId entityId;
        switch (dynamicValue.getSourceType()) {
            case CURRENT_TENANT:
                entityId = user.getTenantId();
                break;
            case CURRENT_CUSTOMER:
                entityId = user.getCustomerId();
                break;
            case CURRENT_USER:
                entityId = user.getId();
                break;
            default:
                throw new RuntimeException("Not supported operation for source type: {" + dynamicValue.getSourceType() + "}");
        }

        try {
            Optional<AttributeKvEntry> valueOpt = attributesService.find(user.getTenantId(), entityId,
                                                                         TbAttributeSubscriptionScope.SERVER_SCOPE.name(), dynamicValue.getSourceAttribute()).get();

            if (valueOpt.isPresent()) {
                @NotNull AttributeKvEntry entry = valueOpt.get();
                @org.jetbrains.annotations.Nullable Object resolved = null;
                switch (predicateType) {
                    case STRING:
                        resolved = KvUtil.getStringValue(entry);
                        break;
                    case NUMERIC:
                        resolved = KvUtil.getDoubleValue(entry);
                        break;
                    case BOOLEAN:
                        resolved = KvUtil.getBoolValue(entry);
                        break;
                    case COMPLEX:
                        break;
                }

                dynamicValue.setResolvedValue((T) resolved);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public PageData<AlarmData> findAlarmDataByQuery(@NotNull SecurityUser securityUser, @NotNull AlarmDataQuery query) {
        @NotNull EntityDataQuery entityDataQuery = this.buildEntityDataQuery(query);
        PageData<EntityData> entities = entityService.findEntityDataByQuery(securityUser.getTenantId(),
                securityUser.getCustomerId(), entityDataQuery);
        if (entities.getTotalElements() > 0) {
            @NotNull LinkedHashMap<EntityId, EntityData> entitiesMap = new LinkedHashMap<>();
            for (@NotNull EntityData entityData : entities.getData()) {
                entitiesMap.put(entityData.getEntityId(), entityData);
            }
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(), query, entitiesMap.keySet());
            for (@NotNull AlarmData alarmData : alarms.getData()) {
                EntityId entityId = alarmData.getEntityId();
                if (entityId != null) {
                    EntityData entityData = entitiesMap.get(entityId);
                    if (entityData != null) {
                        alarmData.getLatest().putAll(entityData.getLatest());
                    }
                }
            }
            return alarms;
        } else {
            return new PageData<>();
        }
    }

    @NotNull
    private EntityDataQuery buildEntityDataQuery(@NotNull AlarmDataQuery query) {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY));
        } else {
            entitiesSortOrder = sortOrder;
        }
        @NotNull EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }

    @NotNull
    @Override
    public DeferredResult<ResponseEntity> getKeysByQuery(@NotNull SecurityUser securityUser, TenantId tenantId, @NotNull EntityDataQuery query,
                                                         boolean isTimeseries, boolean isAttributes) {
        @NotNull final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        if (!isAttributes && !isTimeseries) {
            replyWithEmptyResponse(response);
            return response;
        }

        @NotNull List<EntityId> ids = this.findEntityDataByQuery(securityUser, query).getData().stream()
                                          .map(EntityData::getEntityId)
                                          .collect(Collectors.toList());
        if (ids.isEmpty()) {
            replyWithEmptyResponse(response);
            return response;
        }

        @NotNull Set<EntityType> types = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());
        @org.jetbrains.annotations.Nullable final ListenableFuture<List<String>> timeseriesKeysFuture;
        @org.jetbrains.annotations.Nullable final ListenableFuture<List<String>> attributesKeysFuture;

        if (isTimeseries) {
            timeseriesKeysFuture = dbCallbackExecutor.submit(() -> timeseriesService.findAllKeysByEntityIds(tenantId, ids));
        } else {
            timeseriesKeysFuture = null;
        }

        if (isAttributes) {
            @NotNull Map<EntityType, List<EntityId>> typesMap = ids.stream().collect(Collectors.groupingBy(EntityId::getEntityType));
            @NotNull List<ListenableFuture<List<String>>> futures = new ArrayList<>(typesMap.size());
            typesMap.forEach((type, entityIds) -> futures.add(dbCallbackExecutor.submit(() -> attributesService.findAllKeysByEntityIds(tenantId, type, entityIds))));
            attributesKeysFuture = Futures.transform(Futures.allAsList(futures), lists -> {
                if (CollectionUtils.isEmpty(lists)) {
                    return Collections.emptyList();
                }
                return lists.stream().flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
            }, dbCallbackExecutor);
        } else {
            attributesKeysFuture = null;
        }

        if (isTimeseries && isAttributes) {
            Futures.whenAllComplete(timeseriesKeysFuture, attributesKeysFuture).run(() -> {
                try {
                    replyWithResponse(response, types, timeseriesKeysFuture.get(), attributesKeysFuture.get());
                } catch (Exception e) {
                    log.error("Failed to fetch timeseries and attributes keys!", e);
                    AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }, dbCallbackExecutor);
        } else if (isTimeseries) {
            addCallback(timeseriesKeysFuture, keys -> replyWithResponse(response, types, keys, null),
                    error -> {
                        log.error("Failed to fetch timeseries keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        } else {
            addCallback(attributesKeysFuture, keys -> replyWithResponse(response, types, null, keys),
                    error -> {
                        log.error("Failed to fetch attributes keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
        return response;
    }

    private void replyWithResponse(@NotNull DeferredResult<ResponseEntity> response, @NotNull Set<EntityType> types, @NotNull List<String> timeseriesKeys, @NotNull List<String> attributesKeys) {
        ObjectNode json = JacksonUtil.newObjectNode();
        addItemsToArrayNode(json.putArray("entityTypes"), types);
        addItemsToArrayNode(json.putArray("timeseries"), timeseriesKeys);
        addItemsToArrayNode(json.putArray("attribute"), attributesKeys);
        response.setResult(new ResponseEntity<>(json, HttpStatus.OK));
    }

    private void replyWithEmptyResponse(@NotNull DeferredResult<ResponseEntity> response) {
        replyWithResponse(response, Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
    }

    private void addItemsToArrayNode(@NotNull ArrayNode arrayNode, @NotNull Collection<?> collection) {
        if (!CollectionUtils.isEmpty(collection)) {
            collection.forEach(item -> arrayNode.add(item.toString()));
        }
    }

    private void addCallback(@NotNull ListenableFuture<List<String>> future, @NotNull Consumer<List<String>> success, @NotNull Consumer<Throwable> error) {
        Futures.addCallback(future, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> keys) {
                success.accept(keys);
            }

            @Override
            public void onFailure(Throwable t) {
                error.accept(t);
            }
        }, dbCallbackExecutor);
    }

}
