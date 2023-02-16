package org.echoiot.server.dao.rpc;

import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rpc.Rpc;
import org.echoiot.server.common.data.rpc.RpcStatus;
import org.echoiot.server.dao.Dao;

public interface RpcDao extends Dao<Rpc> {
    PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink);

    PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink);

    PageData<Rpc> findAllRpcByTenantId(TenantId tenantId, PageLink pageLink);

    Long deleteOutdatedRpcByTenantId(TenantId tenantId, Long expirationTime);
}
