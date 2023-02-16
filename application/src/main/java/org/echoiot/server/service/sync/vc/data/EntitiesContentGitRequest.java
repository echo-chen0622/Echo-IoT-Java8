package org.echoiot.server.service.sync.vc.data;

import lombok.Getter;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;

import java.util.List;

@Getter
public class EntitiesContentGitRequest extends PendingGitRequest<List<EntityExportData>> {

    private final String versionId;
    private final EntityType entityType;

    public EntitiesContentGitRequest(TenantId tenantId, String versionId, EntityType entityType) {
        super(tenantId);
        this.versionId = versionId;
        this.entityType = entityType;
    }
}
