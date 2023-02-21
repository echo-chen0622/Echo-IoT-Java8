package org.echoiot.server.dao.sql.event;

import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.dao.model.sql.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EventRepository<T extends EventEntity<V>, V extends Event> {

    List<T> findLatestEvents(UUID tenantId, UUID entityId, int limit);

    Page<T> findEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime, Pageable pageable);

    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

}
