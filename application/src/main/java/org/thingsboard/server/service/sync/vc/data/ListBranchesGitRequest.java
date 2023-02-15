package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;

import java.util.List;

public class ListBranchesGitRequest extends PendingGitRequest<List<BranchInfo>> {

    public ListBranchesGitRequest(TenantId tenantId) {
        super(tenantId);
    }

}
