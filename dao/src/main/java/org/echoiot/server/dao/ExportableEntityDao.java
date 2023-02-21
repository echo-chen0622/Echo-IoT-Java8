package org.echoiot.server.dao;

import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ExportableEntityDao<I extends EntityId, T extends ExportableEntity<?>> extends Dao<T> {

    T findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

    @Nullable
    default T findByTenantIdAndName(UUID tenantId, String name) { throw new UnsupportedOperationException(); }

    PageData<T> findByTenantId(UUID tenantId, PageLink pageLink);

    @Nullable
    I getExternalIdByInternal(I internalId);

}
