package org.echoiot.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface EdgeEventDao.
 */
public interface EdgeEventDao extends Dao<EdgeEvent> {

    /**
     * Save or update edge event object
     *
     * @param edgeEvent the event object
     * @return saved edge event object future
     */
    ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent);


    /**
     * Find edge events by tenantId, edgeId and pageLink.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param pageLink the pageLink
     * @return the event list
     */
    PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate);

    /**
     * Executes stored procedure to cleanup old edge events.
     * @param ttl the ttl for edge events in seconds
     */
    void cleanupEvents(long ttl);

    void migrateEdgeEvents();

}
