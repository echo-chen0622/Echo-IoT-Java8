package org.echoiot.server.service.sync.vc.data;

import org.echoiot.server.common.data.id.TenantId;

public class VoidGitRequest extends PendingGitRequest<Void> {

    public VoidGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
