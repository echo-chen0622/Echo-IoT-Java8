package org.echoiot.server.service.sync.vc.data;

import org.echoiot.server.common.data.id.TenantId;

public class ClearRepositoryGitRequest extends VoidGitRequest {

    public ClearRepositoryGitRequest(TenantId tenantId) {
        super(tenantId);
    }

    public boolean requiresSettings() {
        return false;
    }

}
