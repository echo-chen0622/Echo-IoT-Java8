package org.echoiot.server.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.query.EntityCountQuery;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.entity.EntityService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketService;
import org.echoiot.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.echoiot.server.service.telemetry.cmd.v2.EntityCountUpdate;

@Slf4j
public class TbEntityCountSubCtx extends TbAbstractSubCtx<EntityCountQuery> {

    private volatile int result;

    public TbEntityCountSubCtx(String serviceId, TelemetryWebSocketService wsService, EntityService entityService,
                               TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                               SubscriptionServiceStatistics stats, TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
    }

    @Override
    public void fetchData() {
        result = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        sendWsMsg(new EntityCountUpdate(cmdId, result));
    }

    @Override
    protected void update() {
        int newCount = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new EntityCountUpdate(cmdId, result));
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
