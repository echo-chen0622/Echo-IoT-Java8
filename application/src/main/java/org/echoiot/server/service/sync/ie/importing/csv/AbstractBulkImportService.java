package org.echoiot.server.service.sync.ie.importing.csv;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.echoiot.common.util.DonAsynchron;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.HasId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UUIDBased;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.BasicTsKvEntry;
import org.echoiot.server.common.data.kv.DataType;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.controller.BaseController;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.service.action.EntityActionService;
import org.echoiot.server.service.security.AccessValidator;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.AccessControlService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.echoiot.server.service.telemetry.TelemetrySubscriptionService;
import org.echoiot.server.utils.CsvUtils;
import org.echoiot.server.utils.TypeCastUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractBulkImportService<E extends HasId<? extends EntityId> & HasTenantId> {
    @Resource
    private TelemetrySubscriptionService tsSubscriptionService;
    @Resource
    private TbTenantProfileCache tenantProfileCache;
    @Resource
    private AccessControlService accessControlService;
    @Resource
    private AccessValidator accessValidator;
    @Resource
    private EntityActionService entityActionService;

    private ThreadPoolExecutor executor;

    @PostConstruct
    private void initExecutor() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
                    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(150_000),
                    EchoiotThreadFactory.forName("bulk-import"), new ThreadPoolExecutor.CallerRunsPolicy());
            executor.allowCoreThreadTimeOut(true);
        }
    }

    @NotNull
    public final BulkImportResult<E> processBulkImport(@NotNull BulkImportRequest request, @NotNull SecurityUser user) throws Exception {
        @NotNull List<EntityData> entitiesData = parseData(request);

        @NotNull BulkImportResult<E> result = new BulkImportResult<>();
        @NotNull CountDownLatch completionLatch = new CountDownLatch(entitiesData.size());

        SecurityContext securityContext = SecurityContextHolder.getContext();

        entitiesData.forEach(entityData -> DonAsynchron.submit(() -> {
                    SecurityContextHolder.setContext(securityContext);

                    @NotNull ImportedEntityInfo<E> importedEntityInfo = saveEntity(entityData.getFields(), user);
                    E entity = importedEntityInfo.getEntity();

                    saveKvs(user, entity, entityData.getKvs());

                    return importedEntityInfo;
                },
                importedEntityInfo -> {
                    if (importedEntityInfo.isUpdated()) {
                        result.getUpdated().incrementAndGet();
                    } else {
                        result.getCreated().incrementAndGet();
                    }
                    completionLatch.countDown();
                },
                throwable -> {
                    result.getErrors().incrementAndGet();
                    result.getErrorsList().add(String.format("Line %d: %s", entityData.getLineNumber(), ExceptionUtils.getRootCauseMessage(throwable)));
                    completionLatch.countDown();
                },
                executor));

        completionLatch.await();
        return result;
    }

    @NotNull
    @SneakyThrows
    private ImportedEntityInfo<E> saveEntity(@NotNull Map<BulkImportColumnType, String> fields, @NotNull SecurityUser user) {
        @NotNull ImportedEntityInfo<E> importedEntityInfo = new ImportedEntityInfo<>();

        E entity = findOrCreateEntity(user.getTenantId(), fields.get(BulkImportColumnType.NAME));
        if (entity.getId() != null) {
            importedEntityInfo.setOldEntity((E) entity.getClass().getConstructor(entity.getClass()).newInstance(entity));
            importedEntityInfo.setUpdated(true);
        } else {
            setOwners(entity, user);
        }

        setEntityFields(entity, fields);
        accessControlService.checkPermission(user, PerResource.of(getEntityType()), Operation.WRITE, entity.getId(), entity);

        E savedEntity = saveEntity(user, entity, fields);

        importedEntityInfo.setEntity(savedEntity);
        return importedEntityInfo;
    }


    protected abstract E findOrCreateEntity(TenantId tenantId, String name);

    protected abstract void setOwners(E entity, SecurityUser user);

    protected abstract void setEntityFields(E entity, Map<BulkImportColumnType, String> fields);

    protected abstract E saveEntity(SecurityUser user, E entity, Map<BulkImportColumnType, String> fields);

    protected abstract EntityType getEntityType();

    protected ObjectNode getOrCreateAdditionalInfoObj(@NotNull HasAdditionalInfo entity) {
        return entity.getAdditionalInfo() == null || entity.getAdditionalInfo().isNull() ?
                JacksonUtil.newObjectNode() : (ObjectNode) entity.getAdditionalInfo();
    }

    private void saveKvs(@NotNull SecurityUser user, @NotNull E entity, @NotNull Map<BulkImportRequest.ColumnMapping, ParsedValue> data) {
        Arrays.stream(BulkImportColumnType.values())
                .filter(BulkImportColumnType::isKv)
                .map(kvType -> {
                    @NotNull JsonObject kvs = new JsonObject();
                    data.entrySet().stream()
                            .filter(dataEntry -> dataEntry.getKey().getType() == kvType &&
                                                 StringUtils.isNotEmpty(dataEntry.getKey().getKey()))
                            .forEach(dataEntry -> kvs.add(dataEntry.getKey().getKey(), dataEntry.getValue().toJsonPrimitive()));
                    return Map.entry(kvType, kvs);
                })
                .filter(kvsEntry -> kvsEntry.getValue().entrySet().size() > 0)
                .forEach(kvsEntry -> {
                    BulkImportColumnType kvType = kvsEntry.getKey();
                    if (kvType == BulkImportColumnType.SHARED_ATTRIBUTE || kvType == BulkImportColumnType.SERVER_ATTRIBUTE) {
                        saveAttributes(user, entity, kvsEntry, kvType);
                    } else {
                        saveTelemetry(user, entity, kvsEntry);
                    }
                });
    }

    @SneakyThrows
    private void saveTelemetry(@NotNull SecurityUser user, @NotNull E entity, @NotNull Map.Entry<BulkImportColumnType, JsonObject> kvsEntry) {
        @NotNull List<TsKvEntry> timeseries = JsonConverter.convertToTelemetry(kvsEntry.getValue(), System.currentTimeMillis())
                                                           .entrySet().stream()
                                                           .flatMap(entry -> entry.getValue().stream().map(kvEntry -> new BasicTsKvEntry(entry.getKey(), kvEntry)))
                                                           .collect(Collectors.toList());

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entity.getId(), (result, tenantId, entityId) -> {
            @org.jetbrains.annotations.Nullable TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            long tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            tsSubscriptionService.saveAndNotify(tenantId, user.getCustomerId(), entityId, timeseries, tenantTtl, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                                                        ActionType.TIMESERIES_UPDATED, null, timeseries);
                }

                @Override
                public void onFailure(Throwable t) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                            ActionType.TIMESERIES_UPDATED, BaseController.toException(t), timeseries);
                    throw new RuntimeException(t);
                }
            });
        });
    }

    @SneakyThrows
    private void saveAttributes(SecurityUser user, @NotNull E entity, @NotNull Map.Entry<BulkImportColumnType, JsonObject> kvsEntry, @NotNull BulkImportColumnType kvType) {
        String scope = kvType.getKey();
        @NotNull List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(kvsEntry.getValue()));

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_ATTRIBUTES, entity.getId(), (result, tenantId, entityId) -> {
            tsSubscriptionService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<>() {

                @Override
                public void onSuccess(Void unused) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, null, scope, attributes);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, BaseController.toException(throwable),
                            scope, attributes);
                    throw new RuntimeException(throwable);
                }

            });
        });
    }

    @NotNull
    private List<EntityData> parseData(@NotNull BulkImportRequest request) throws Exception {
        @NotNull List<List<String>> records = CsvUtils.parseCsv(request.getFile(), request.getMapping().getDelimiter());
        @NotNull AtomicInteger linesCounter = new AtomicInteger(0);

        if (request.getMapping().getHeader()) {
            records.remove(0);
            linesCounter.incrementAndGet();
        }

        List<BulkImportRequest.ColumnMapping> columnsMappings = request.getMapping().getColumns();
        return records.stream()
                .map(record -> {
                    @NotNull EntityData entityData = new EntityData();
                    Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                            .map(i -> Map.entry(columnsMappings.get(i), record.get(i)))
                            .filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
                            .forEach(entry -> {
                                if (!entry.getKey().getType().isKv()) {
                                    entityData.getFields().put(entry.getKey().getType(), entry.getValue());
                                } else {
                                    @NotNull Map.Entry<DataType, Object> castResult = TypeCastUtil.castValue(entry.getValue());
                                    entityData.getKvs().put(entry.getKey(), new ParsedValue(castResult.getValue(), castResult.getKey()));
                                }
                            });
                    entityData.setLineNumber(linesCounter.incrementAndGet());
                    return entityData;
                })
                .collect(Collectors.toList());
    }

    @PreDestroy
    private void shutdownExecutor() {
        if (!executor.isTerminating()) {
            executor.shutdown();
        }
    }

    @Data
    protected static class EntityData {
        private final Map<BulkImportColumnType, String> fields = new LinkedHashMap<>();
        private final Map<BulkImportRequest.ColumnMapping, ParsedValue> kvs = new LinkedHashMap<>();
        private int lineNumber;
    }

    @Data
    protected static class ParsedValue {
        @NotNull
        private final Object value;
        @NotNull
        private final DataType dataType;

        @org.jetbrains.annotations.Nullable
        public JsonPrimitive toJsonPrimitive() {
            switch (dataType) {
                case STRING:
                    return new JsonPrimitive((String) value);
                case LONG:
                    return new JsonPrimitive((Long) value);
                case DOUBLE:
                    return new JsonPrimitive((Double) value);
                case BOOLEAN:
                    return new JsonPrimitive((Boolean) value);
                default:
                    return null;
            }
        }

        public String stringValue() {
            return value.toString();
        }

    }

}
