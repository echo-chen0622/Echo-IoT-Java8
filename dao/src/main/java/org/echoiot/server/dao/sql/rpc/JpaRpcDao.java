package org.echoiot.server.dao.sql.rpc;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.RpcEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rpc.Rpc;
import org.echoiot.server.common.data.rpc.RpcStatus;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.rpc.RpcDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaRpcDao extends JpaAbstractDao<RpcEntity, Rpc> implements RpcDao {

    @NotNull
    private final RpcRepository rpcRepository;

    @NotNull
    @Override
    protected Class<RpcEntity> getEntityClass() {
        return RpcEntity.class;
    }

    @Override
    protected JpaRepository<RpcEntity, UUID> getRepository() {
        return rpcRepository;
    }

    @NotNull
    @Override
    public PageData<Rpc> findAllByDeviceId(@NotNull TenantId tenantId, @NotNull DeviceId deviceId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceId(tenantId.getId(), deviceId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<Rpc> findAllByDeviceIdAndStatus(@NotNull TenantId tenantId, @NotNull DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceIdAndStatus(tenantId.getId(), deviceId.getId(), rpcStatus, DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<Rpc> findAllRpcByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Long deleteOutdatedRpcByTenantId(@NotNull TenantId tenantId, Long expirationTime) {
        return rpcRepository.deleteOutdatedRpcByTenantId(tenantId.getId(), expirationTime);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.RPC;
    }

}
