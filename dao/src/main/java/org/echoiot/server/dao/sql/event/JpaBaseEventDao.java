package org.echoiot.server.dao.sql.event;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.EventEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.event.ErrorEventFilter;
import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.common.data.event.EventFilter;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.event.LifeCycleEventFilter;
import org.echoiot.server.common.data.event.RuleChainDebugEventFilter;
import org.echoiot.server.common.data.event.RuleNodeDebugEventFilter;
import org.echoiot.server.common.data.event.StatisticsEventFilter;
import org.echoiot.server.common.data.id.EventId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.stats.StatsFactory;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.event.EventDao;
import org.echoiot.server.dao.sql.ScheduledLogExecutorComponent;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueParams;
import org.echoiot.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.echoiot.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.echoiot.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaBaseEventDao implements EventDao {

    @Resource
    private EventPartitionConfiguration partitionConfiguration;

    @Resource
    private SqlPartitioningRepository partitioningRepository;

    @Resource
    private LifecycleEventRepository lcEventRepository;

    @Resource
    private StatisticsEventRepository statsEventRepository;

    @Resource
    private ErrorEventRepository errorEventRepository;

    @Resource
    private EventInsertRepository eventInsertRepository;

    @Resource
    private EventCleanupRepository eventCleanupRepository;

    @Resource
    private RuleNodeDebugEventRepository ruleNodeDebugEventRepository;

    @Resource
    private RuleChainDebugEventRepository ruleChainDebugEventRepository;

    @Resource
    ScheduledLogExecutorComponent logExecutor;

    @Resource
    private StatsFactory statsFactory;

    @Value("${sql.events.batch_size:10000}")
    private int batchSize;

    @Value("${sql.events.batch_max_delay:100}")
    private long maxDelay;

    @Value("${sql.events.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;

    @Value("${sql.events.batch_threads:3}")
    private int batchThreads;

    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    private TbSqlBlockingQueueWrapper<Event> queue;

    private final Map<EventType, EventRepository<?, ?>> repositories = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Events")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("events")
                .batchSortEnabled(batchSortEnabled)
                .build();
        @NotNull Function<Event, Integer> hashcodeFunction = entity -> Objects.hash(super.hashCode(), entity.getTenantId(), entity.getEntityId());
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, v -> eventInsertRepository.save(v), Comparator.comparing(Event::getCreatedTime));
        repositories.put(EventType.LC_EVENT, lcEventRepository);
        repositories.put(EventType.STATS, statsEventRepository);
        repositories.put(EventType.ERROR, errorEventRepository);
        repositories.put(EventType.DEBUG_RULE_NODE, ruleNodeDebugEventRepository);
        repositories.put(EventType.DEBUG_RULE_CHAIN, ruleChainDebugEventRepository);
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<Void> saveAsync(@NotNull Event event) {
        log.debug("Save event [{}] ", event);
        if (event.getId() == null) {
            @NotNull UUID timeBased = Uuids.timeBased();
            event.setId(new EventId(timeBased));
            event.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (event.getCreatedTime() == 0L) {
            UUID eventId = event.getId().getId();
            if (eventId.version() == 1) {
                event.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                event.setCreatedTime(System.currentTimeMillis());
            }
        }
        partitioningRepository.createPartitionIfNotExists(event.getType().getTable(), event.getCreatedTime(),
                partitionConfiguration.getPartitionSizeInMs(event.getType()));
        return queue.add(event);
    }

    @NotNull
    @Override
    public PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, @NotNull TimePageLink pageLink) {
        return DaoUtil.toPageData(getEventRepository(eventType).findEvents(tenantId, entityId, pageLink.getStartTime(), pageLink.getEndTime(), DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
    }

    @Override
    public PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull EventFilter eventFilter, @NotNull TimePageLink pageLink) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    return findEventByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, pageLink);
                case DEBUG_RULE_CHAIN:
                    return findEventByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, pageLink);
                case LC_EVENT:
                    return findEventByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, pageLink);
                case ERROR:
                    return findEventByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, pageLink);
                case STATS:
                    return findEventByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, pageLink);
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            return findEvents(tenantId, entityId, eventFilter.getEventType(), pageLink);
        }
    }

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime) {
        log.debug("[{}][{}] Remove events [{}-{}] ", tenantId, entityId, startTime, endTime);
        for (EventType eventType : EventType.values()) {
            getEventRepository(eventType).removeEvents(tenantId, entityId, startTime, endTime);
        }
    }

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, @NotNull EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    removeEventsByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case DEBUG_RULE_CHAIN:
                    removeEventsByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case LC_EVENT:
                    removeEventsByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, startTime, endTime);
                    break;
                case ERROR:
                    removeEventsByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, startTime, endTime);
                    break;
                case STATS:
                    removeEventsByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, startTime, endTime);
                    break;
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            getEventRepository(eventFilter.getEventType()).removeEvents(tenantId, entityId, startTime, endTime);
        }
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        eventCleanupRepository.migrateEvents(regularEventTs, debugEventTs);
    }

    @NotNull
    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull RuleChainDebugEventFilter eventFilter, @NotNull TimePageLink pageLink) {
        return DaoUtil.toPageData(
                ruleChainDebugEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMessage(),
                        eventFilter.isError(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
    }

    @NotNull
    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull RuleNodeDebugEventFilter eventFilter, @NotNull TimePageLink pageLink) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");
        return DaoUtil.toPageData(
                ruleNodeDebugEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMsgDirectionType(),
                        eventFilter.getEntityId(),
                        eventFilter.getEntityType(),
                        eventFilter.getMsgId(),
                        eventFilter.getMsgType(),
                        eventFilter.getRelationType(),
                        eventFilter.getDataSearch(),
                        eventFilter.getMetadataSearch(),
                        eventFilter.isError(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap)));
    }

    @NotNull
    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull ErrorEventFilter eventFilter, @NotNull TimePageLink pageLink) {
        return DaoUtil.toPageData(
                errorEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMethod(),
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
        );
    }

    @NotNull
    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull LifeCycleEventFilter eventFilter, @NotNull TimePageLink pageLink) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");
        return DaoUtil.toPageData(
                lcEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getEvent(),
                        statusFilterEnabled,
                        statusFilter,
                        eventFilter.getErrorStr(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
        );
    }

    @NotNull
    private PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, @NotNull StatisticsEventFilter eventFilter, @NotNull TimePageLink pageLink) {
        return DaoUtil.toPageData(
                statsEventRepository.findEvents(
                        tenantId,
                        entityId,
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        eventFilter.getServer(),
                        eventFilter.getMinMessagesProcessed(),
                        eventFilter.getMaxMessagesProcessed(),
                        eventFilter.getMinErrorsOccurred(),
                        eventFilter.getMaxErrorsOccurred(),
                        DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap))
        );
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, @NotNull RuleChainDebugEventFilter eventFilter, Long startTime, Long endTime) {
        ruleChainDebugEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMessage(),
                eventFilter.isError(),
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, @NotNull RuleNodeDebugEventFilter eventFilter, Long startTime, Long endTime) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");
        ruleNodeDebugEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMsgDirectionType(),
                eventFilter.getEntityId(),
                eventFilter.getEntityType(),
                eventFilter.getMsgId(),
                eventFilter.getMsgType(),
                eventFilter.getRelationType(),
                eventFilter.getDataSearch(),
                eventFilter.getMetadataSearch(),
                eventFilter.isError(),
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, @NotNull ErrorEventFilter eventFilter, Long startTime, Long endTime) {
        errorEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMethod(),
                eventFilter.getErrorStr());

    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, @NotNull LifeCycleEventFilter eventFilter, Long startTime, Long endTime) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");
        lcEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getEvent(),
                statusFilterEnabled,
                statusFilter,
                eventFilter.getErrorStr());
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, @NotNull StatisticsEventFilter eventFilter, Long startTime, Long endTime) {
        statsEventRepository.removeEvents(
                tenantId,
                entityId,
                startTime,
                endTime,
                eventFilter.getServer(),
                eventFilter.getMinMessagesProcessed(),
                eventFilter.getMaxMessagesProcessed(),
                eventFilter.getMinErrorsOccurred(),
                eventFilter.getMaxErrorsOccurred()
        );
    }

    @Override
    public List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit) {
        return DaoUtil.convertDataList(getEventRepository(eventType).findLatestEvents(tenantId, entityId, limit));
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        if (regularEventExpTs > 0) {
            log.info("Going to cleanup regular events with exp time: {}", regularEventExpTs);
            if (cleanupDb) {
                eventCleanupRepository.cleanupEvents(regularEventExpTs, false);
            } else {
                cleanupPartitionsCache(regularEventExpTs, false);
            }
        }
        if (debugEventExpTs > 0) {
            log.info("Going to cleanup debug events with exp time: {}", debugEventExpTs);
            if (cleanupDb) {
                eventCleanupRepository.cleanupEvents(debugEventExpTs, true);
            } else {
                cleanupPartitionsCache(debugEventExpTs, true);
            }
        }
    }

    private void cleanupPartitionsCache(long expTime, boolean isDebug) {
        for (@NotNull EventType eventType : EventType.values()) {
            if (eventType.isDebug() == isDebug) {
                partitioningRepository.cleanupPartitionsCache(eventType.getTable(), expTime, partitionConfiguration.getPartitionSizeInMs(eventType));
            }
        }
    }

    private void parseUUID(@NotNull String src, String paramName) {
        if (!StringUtils.isEmpty(src)) {
            try {
                UUID.fromString(src);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to convert " + paramName + " to UUID!");
            }
        }
    }

    @NotNull
    private EventRepository<? extends EventEntity<?>, ?> getEventRepository(EventType eventType) {
        var repository = repositories.get(eventType);
        if (repository == null) {
            throw new RuntimeException("Event type: " + eventType + " is not supported!");
        }
        return repository;
    }


}
