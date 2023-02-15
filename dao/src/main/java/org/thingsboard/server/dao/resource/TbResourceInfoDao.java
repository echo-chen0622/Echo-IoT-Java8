package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface TbResourceInfoDao extends Dao<TbResourceInfo> {

    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<TbResourceInfo> findTenantResourcesByTenantId(UUID tenantId, PageLink pageLink);

}
