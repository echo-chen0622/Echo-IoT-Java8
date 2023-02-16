package org.echoiot.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;

public interface EdgeEventService {

    ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent);

    PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate);

    /**
     * Executes stored procedure to cleanup old edge events.
     * @param ttl the ttl for edge events in seconds
     */
    void cleanupEvents(long ttl);
}
