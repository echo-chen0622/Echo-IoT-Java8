package org.echoiot.server.service.query;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.service.security.model.SecurityUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public interface EntityQueryService {

    long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query);

    PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query);

    PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query);

    DeferredResult<ResponseEntity> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                  boolean isTimeseries, boolean isAttributes);

}
