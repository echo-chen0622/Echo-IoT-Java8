package org.echoiot.server.service.sync.vc.data;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.BranchInfo;

import java.util.List;

public class ListBranchesGitRequest extends PendingGitRequest<List<BranchInfo>> {

    public ListBranchesGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
