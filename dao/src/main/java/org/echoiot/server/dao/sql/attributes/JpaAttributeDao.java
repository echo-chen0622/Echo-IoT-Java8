package org.echoiot.server.dao.sql.attributes;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.attributes.AttributesDao;
import org.echoiot.server.dao.model.sql.AttributeKvCompositeKey;
import org.echoiot.server.dao.model.sql.AttributeKvEntity;
import org.echoiot.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.echoiot.server.dao.sql.ScheduledLogExecutorComponent;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueParams;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@SqlDao
public class JpaAttributeDao extends JpaAbstractDaoListeningExecutorService implements AttributesDao {

    @Resource
    ScheduledLogExecutorComponent logExecutor;

    @Resource
    private AttributeKvRepository attributeKvRepository;

    @Resource
    private AttributeKvInsertRepository attributeKvInsertRepository;

    @Resource
    private StatsFactory statsFactory;

    @Value("${sql.attributes.batch_size:1000}")
    private int batchSize;

    @Value("${sql.attributes.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.attributes.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    @Value("${sql.attributes.batch_threads:4}")
    private int batchThreads;

    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    private TbSqlBlockingQueueWrapper<AttributeKvEntity> queue;

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Attributes")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("attributes")
                .batchSortEnabled(batchSortEnabled)
                .build();

        Function<AttributeKvEntity, Integer> hashcodeFunction = entity -> entity.getId().getEntityId().hashCode();
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, v -> attributeKvInsertRepository.saveOrUpdate(v),
                Comparator.comparing((AttributeKvEntity attributeKvEntity) -> attributeKvEntity.getId().getEntityId())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getEntityType().name())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeType())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeKey())
        );
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey) {
        AttributeKvCompositeKey compositeKey =
                getAttributeKvCompositeKey(entityId, attributeType, attributeKey);
        return Optional.ofNullable(DaoUtil.getData(attributeKvRepository.findById(compositeKey)));
    }

    @Override
    public List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKeys) {
        List<AttributeKvCompositeKey> compositeKeys =
                attributeKeys
                        .stream()
                        .map(attributeKey ->
                                getAttributeKvCompositeKey(entityId, attributeType, attributeKey))
                        .collect(Collectors.toList());
        return DaoUtil.convertDataList(Lists.newArrayList(attributeKvRepository.findAllById(compositeKeys)));
    }

    @Override
    public List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, String attributeType) {
        return DaoUtil.convertDataList(Lists.newArrayList(
                        attributeKvRepository.findAllByEntityTypeAndEntityIdAndAttributeType(
                                entityId.getEntityType(),
                                entityId.getId(),
                                attributeType)));
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, @Nullable DeviceProfileId deviceProfileId) {
        if (deviceProfileId != null) {
            return attributeKvRepository.findAllKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId());
        } else {
            return attributeKvRepository.findAllKeysByTenantId(tenantId.getId());
        }
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributeKvRepository
                .findAllKeysByEntityIds(entityType.name(), entityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
    }

    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setId(new AttributeKvCompositeKey(entityId.getEntityType(), entityId.getId(), attributeType, attribute.getKey()));
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrValue(attribute.getStrValue().orElse(null));
        entity.setDoubleValue(attribute.getDoubleValue().orElse(null));
        entity.setLongValue(attribute.getLongValue().orElse(null));
        entity.setBooleanValue(attribute.getBooleanValue().orElse(null));
        entity.setJsonValue(attribute.getJsonValue().orElse(null));
        return addToQueue(entity);
    }

    private ListenableFuture<String> addToQueue(AttributeKvEntity entity) {
        return Futures.transform(queue.add(entity), v -> entity.getId().getAttributeKey(), MoreExecutors.directExecutor());
    }

    @Override
    public List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys) {
        List<ListenableFuture<String>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                attributeKvRepository.delete(entityId.getEntityType(), entityId.getId(), attributeType, key);
                return key;
            }));
        }
        return futuresList;
    }

    private AttributeKvCompositeKey getAttributeKvCompositeKey(EntityId entityId, String attributeType, String attributeKey) {
        return new AttributeKvCompositeKey(
                entityId.getEntityType(),
                entityId.getId(),
                attributeType,
                attributeKey);
    }
}
