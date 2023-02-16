package org.echoiot.server.service.sync.vc.data;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.sync.vc.EntityVersion;

public class ListVersionsGitRequest extends PendingGitRequest<PageData<EntityVersion>> {

    public ListVersionsGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
