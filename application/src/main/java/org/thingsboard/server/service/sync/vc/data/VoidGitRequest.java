package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;

public class VoidGitRequest extends PendingGitRequest<Void> {

    public VoidGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
