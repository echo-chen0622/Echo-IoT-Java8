package org.echoiot.server.service.sync.vc.data;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.VersionedEntityInfo;

import java.util.List;

public class ListEntitiesGitRequest extends PendingGitRequest<List<VersionedEntityInfo>> {

    public ListEntitiesGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
