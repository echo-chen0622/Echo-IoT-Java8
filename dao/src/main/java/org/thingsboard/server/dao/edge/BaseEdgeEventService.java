package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.DataValidator;

@Service
@Slf4j
@AllArgsConstructor
public class BaseEdgeEventService implements EdgeEventService {

    private final EdgeEventDao edgeEventDao;

    private final DataValidator<EdgeEvent> edgeEventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);
        return edgeEventDao.saveAsync(edgeEvent);
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        return edgeEventDao.findEdgeEvents(tenantId.getId(), edgeId, pageLink, withTsUpdate);
    }

    @Override
    public void cleanupEvents(long ttl) {
        edgeEventDao.cleanupEvents(ttl);
    }
}
