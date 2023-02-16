package org.echoiot.server.dao.resource;

import org.echoiot.server.common.data.TbResourceInfo;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;

import java.util.UUID;

public interface TbResourceInfoDao extends Dao<TbResourceInfo> {

    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<TbResourceInfo> findTenantResourcesByTenantId(UUID tenantId, PageLink pageLink);

}
