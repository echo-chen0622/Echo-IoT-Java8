package org.echoiot.server.dao;

import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

import java.util.UUID;

public interface ExportableEntityDao<I extends EntityId, T extends ExportableEntity<?>> extends Dao<T> {

    T findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

    default T findByTenantIdAndName(UUID tenantId, String name) { throw new UnsupportedOperationException(); }

    PageData<T> findByTenantId(UUID tenantId, PageLink pageLink);

    I getExternalIdByInternal(I internalId);

}
