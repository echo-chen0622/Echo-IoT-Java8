package org.echoiot.server.dao.sql.query;

import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.common.data.query.AlarmDataQuery;

import java.util.Collection;

public interface AlarmQueryRepository {

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                        AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

}
