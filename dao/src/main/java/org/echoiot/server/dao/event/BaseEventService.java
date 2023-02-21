package org.echoiot.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.event.ErrorEvent;
import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.common.data.event.EventFilter;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.event.LifecycleEvent;
import org.echoiot.server.common.data.event.RuleChainDebugEvent;
import org.echoiot.server.common.data.event.RuleNodeDebugEvent;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaseEventService implements EventService {

    @Value("${sql.ttl.events.events_ttl:0}")
    private long ttlInSec;
    @Value("${sql.ttl.events.debug_events_ttl:604800}")
    private long debugTtlInSec;

    @Value("${event.debug.max-symbols:4096}")
    private int maxDebugEventSymbols;

    @Resource
    public EventDao eventDao;

    @Resource
    private DataValidator<Event> eventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(@NotNull Event event) {
        eventValidator.validate(event, Event::getTenantId);
        checkAndTruncateDebugEvent(event);
        return eventDao.saveAsync(event);
    }

    private void checkAndTruncateDebugEvent(@NotNull Event event) {
        switch (event.getType()) {
            case DEBUG_RULE_NODE:
                @NotNull RuleNodeDebugEvent rnEvent = (RuleNodeDebugEvent) event;
                truncateField(rnEvent, RuleNodeDebugEvent::getData, RuleNodeDebugEvent::setData);
                truncateField(rnEvent, RuleNodeDebugEvent::getMetadata, RuleNodeDebugEvent::setMetadata);
                truncateField(rnEvent, RuleNodeDebugEvent::getError, RuleNodeDebugEvent::setError);
                break;
            case DEBUG_RULE_CHAIN:
                @NotNull RuleChainDebugEvent rcEvent = (RuleChainDebugEvent) event;
                truncateField(rcEvent, RuleChainDebugEvent::getMessage, RuleChainDebugEvent::setMessage);
                truncateField(rcEvent, RuleChainDebugEvent::getError, RuleChainDebugEvent::setError);
                break;
            case LC_EVENT:
                @NotNull LifecycleEvent lcEvent = (LifecycleEvent) event;
                truncateField(lcEvent, LifecycleEvent::getError, LifecycleEvent::setError);
                break;
            case ERROR:
                @NotNull ErrorEvent eEvent = (ErrorEvent) event;
                truncateField(eEvent, ErrorEvent::getError, ErrorEvent::setError);
                break;
        }
    }

    private <T extends Event> void truncateField(T event, @NotNull Function<T, String> getter, @NotNull BiConsumer<T, String> setter) {
        var str = getter.apply(event);
        if (StringUtils.isNotEmpty(str)) {
            var length = str.length();
            if (length > maxDebugEventSymbols) {
                setter.accept(event, str.substring(0, maxDebugEventSymbols) + "...[truncated " + (length - maxDebugEventSymbols) + " symbols]");
            }
        }
    }

    @NotNull
    @Override
    public PageData<EventInfo> findEvents(@NotNull TenantId tenantId, @NotNull EntityId entityId, EventType eventType, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, pageLink));
    }

    @Nullable
    @Override
    public List<EventInfo> findLatestEvents(@NotNull TenantId tenantId, @NotNull EntityId entityId, EventType eventType, int limit) {
        return convert(entityId.getEntityType(), eventDao.findLatestEvents(tenantId.getId(), entityId.getId(), eventType, limit));
    }

    @NotNull
    @Override
    public PageData<EventInfo> findEventsByFilter(@NotNull TenantId tenantId, @NotNull EntityId entityId, EventFilter eventFilter, TimePageLink pageLink) {
        return convert(entityId.getEntityType(), eventDao.findEventByFilter(tenantId.getId(), entityId.getId(), eventFilter, pageLink));
    }

    @Override
    public void removeEvents(@NotNull TenantId tenantId, @NotNull EntityId entityId) {
        removeEvents(tenantId, entityId, null, null, null);
    }

    @Override
    public void removeEvents(@NotNull TenantId tenantId, @NotNull EntityId entityId, @Nullable EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter == null) {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), startTime, endTime);
        } else {
            eventDao.removeEvents(tenantId.getId(), entityId.getId(), eventFilter, startTime, endTime);
        }
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        eventDao.cleanupEvents(regularEventExpTs, debugEventExpTs, cleanupDb);
    }

    @Override
    public void migrateEvents() {
        eventDao.migrateEvents(ttlInSec > 0 ? (System.currentTimeMillis() - ttlInSec * 1000) : 0, debugTtlInSec > 0 ? (System.currentTimeMillis() - debugTtlInSec * 1000) : 0);
    }

    @NotNull
    private PageData<EventInfo> convert(EntityType entityType, @NotNull PageData<? extends Event> pd) {
        return new PageData<>(pd.getData() == null ? null :
                pd.getData().stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList())
                , pd.getTotalPages(), pd.getTotalElements(), pd.hasNext());
    }

    private List<EventInfo> convert(EntityType entityType, @Nullable List<? extends Event> list) {
        return list == null ? null : list.stream().map(e -> e.toInfo(entityType)).collect(Collectors.toList());
    }
}
