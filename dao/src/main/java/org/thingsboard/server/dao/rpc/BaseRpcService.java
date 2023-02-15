package org.thingsboard.server.dao.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.service.PaginatedRemover;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseRpcService implements RpcService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_RPC_ID = "Incorrect rpcId ";

    private final RpcDao rpcDao;

    @Override
    public Rpc save(Rpc rpc) {
        log.trace("Executing save, [{}]", rpc);
        return rpcDao.save(rpc.getTenantId(), rpc);
    }

    @Override
    public void deleteRpc(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing deleteRpc, tenantId [{}], rpcId [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        rpcDao.removeById(tenantId, rpcId.getId());
    }

    @Override
    public void deleteAllRpcByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAllRpcByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantRpcRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public Rpc findById(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing findById, tenantId [{}], rpcId [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        return rpcDao.findById(tenantId, rpcId.getId());
    }

    @Override
    public ListenableFuture<Rpc> findRpcByIdAsync(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing findRpcByIdAsync, tenantId [{}], rpcId: [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        return rpcDao.findByIdAsync(tenantId, rpcId.getId());
    }

    @Override
    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        log.trace("Executing findAllByDeviceIdAndStatus, tenantId [{}], deviceId [{}], rpcStatus [{}], pageLink [{}]", tenantId, deviceId, rpcStatus, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return rpcDao.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

    @Override
    public PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        log.trace("Executing findAllByDeviceIdAndStatus, tenantId [{}], deviceId [{}], pageLink [{}]", tenantId, deviceId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return rpcDao.findAllByDeviceId(tenantId, deviceId, pageLink);
    }

    private PaginatedRemover<TenantId, Rpc> tenantRpcRemover =
            new PaginatedRemover<>() {
                @Override
                protected PageData<Rpc> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return rpcDao.findAllRpcByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Rpc entity) {
                    deleteRpc(tenantId, entity.getId());
                }
            };
}
