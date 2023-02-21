package org.echoiot.server.dao.sql.usagerecord;

import org.echoiot.server.dao.model.sql.ApiUsageStateEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.ApiUsageState;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.usagerecord.ApiUsageStateDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * @author Andrii Shvaika
 */
@Component
@SqlDao
public class JpaApiUsageStateDao extends JpaAbstractDao<ApiUsageStateEntity, ApiUsageState> implements ApiUsageStateDao {

    private final ApiUsageStateRepository apiUsageStateRepository;

    public JpaApiUsageStateDao(ApiUsageStateRepository apiUsageStateRepository) {
        this.apiUsageStateRepository = apiUsageStateRepository;
    }

    @NotNull
    @Override
    protected Class<ApiUsageStateEntity> getEntityClass() {
        return ApiUsageStateEntity.class;
    }

    @Override
    protected JpaRepository<ApiUsageStateEntity, UUID> getRepository() {
        return apiUsageStateRepository;
    }

    @Override
    public ApiUsageState findTenantApiUsageState(UUID tenantId) {
        return DaoUtil.getData(apiUsageStateRepository.findByTenantId(tenantId));
    }

    @Override
    public ApiUsageState findApiUsageStateByEntityId(@NotNull EntityId entityId) {
        return DaoUtil.getData(apiUsageStateRepository.findByEntityIdAndEntityType(entityId.getId(), entityId.getEntityType().name()));
    }

    @Override
    public void deleteApiUsageStateByTenantId(@NotNull TenantId tenantId) {
        apiUsageStateRepository.deleteApiUsageStateByTenantId(tenantId.getId());
    }

    @Override
    public void deleteApiUsageStateByEntityId(@NotNull EntityId entityId) {
        apiUsageStateRepository.deleteByEntityIdAndEntityType(entityId.getId(), entityId.getEntityType().name());
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
