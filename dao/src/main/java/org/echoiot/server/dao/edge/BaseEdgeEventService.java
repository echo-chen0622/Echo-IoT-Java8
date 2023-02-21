package org.echoiot.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class BaseEdgeEventService implements EdgeEventService {

    @NotNull
    private final EdgeEventDao edgeEventDao;

    @NotNull
    private final DataValidator<EdgeEvent> edgeEventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);
        return edgeEventDao.saveAsync(edgeEvent);
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(@NotNull TenantId tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        return edgeEventDao.findEdgeEvents(tenantId.getId(), edgeId, pageLink, withTsUpdate);
    }

    @Override
    public void cleanupEvents(long ttl) {
        edgeEventDao.cleanupEvents(ttl);
    }
}
