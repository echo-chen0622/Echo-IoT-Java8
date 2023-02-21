package org.echoiot.server.dao.sql.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.EntityCountQuery;
import org.echoiot.server.common.data.query.EntityData;
import org.echoiot.server.common.data.query.EntityDataQuery;
import org.echoiot.server.dao.entity.EntityQueryDao;

@Component
public class JpaEntityQueryDao implements EntityQueryDao {

    @Resource
    private EntityQueryRepository entityQueryRepository;

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        return entityQueryRepository.countEntitiesByQuery(tenantId, customerId, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        return entityQueryRepository.findEntityDataByQuery(tenantId, customerId, query);
    }
}
