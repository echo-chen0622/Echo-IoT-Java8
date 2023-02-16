package org.echoiot.server.service.sync.vc.data;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.sync.ie.EntityExportSettings;
import org.echoiot.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.echoiot.server.common.data.sync.vc.request.create.SyncStrategy;
import org.echoiot.server.common.data.sync.vc.request.create.VersionCreateRequest;

public class EntityTypeExportCtx extends EntitiesExportCtx<VersionCreateRequest> {

    @Getter
    private final EntityType entityType;
    @Getter
    private final boolean overwrite;
    @Getter
    private final EntityExportSettings settings;

    public EntityTypeExportCtx(EntitiesExportCtx<?> parent, EntityTypeVersionCreateConfig config, SyncStrategy defaultSyncStrategy, EntityType entityType) {
        super(parent);
        this.entityType = entityType;
        this.settings = EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build();
        this.overwrite = ObjectUtils.defaultIfNull(config.getSyncStrategy(), defaultSyncStrategy) == SyncStrategy.OVERWRITE;
    }

}
