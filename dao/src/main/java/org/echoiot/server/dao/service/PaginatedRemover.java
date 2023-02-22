package org.echoiot.server.dao.service;

import org.echoiot.server.common.data.id.IdBased;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

public abstract class PaginatedRemover<I, D extends IdBased<?>> {

    private static final int DEFAULT_LIMIT = 100;

    public void removeEntities(TenantId tenantId, I id) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            PageData<D> entities = findEntities(tenantId, id, pageLink);
            for (D entity : entities.getData()) {
                removeEntity(tenantId, entity);
            }
            hasNext = entities.hasNext();
        }
    }

    protected abstract PageData<D> findEntities(TenantId tenantId, I id, PageLink pageLink);

    protected abstract void removeEntity(TenantId tenantId, D entity);

}
