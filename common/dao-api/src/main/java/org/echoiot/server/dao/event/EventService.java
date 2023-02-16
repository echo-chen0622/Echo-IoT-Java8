package org.echoiot.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.common.data.event.EventFilter;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;

import java.util.List;

public interface EventService {

    ListenableFuture<Void> saveAsync(Event event);

    PageData<EventInfo> findEvents(TenantId tenantId, EntityId entityId, EventType eventType, TimePageLink pageLink);

    List<EventInfo> findLatestEvents(TenantId tenantId, EntityId entityId, EventType eventType, int limit);

    PageData<EventInfo> findEventsByFilter(TenantId tenantId, EntityId entityId, EventFilter eventFilter, TimePageLink pageLink);

    void removeEvents(TenantId tenantId, EntityId entityId);

    void removeEvents(TenantId tenantId, EntityId entityId, EventFilter eventFilter, Long startTime, Long endTime);

    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

    void migrateEvents();
}
