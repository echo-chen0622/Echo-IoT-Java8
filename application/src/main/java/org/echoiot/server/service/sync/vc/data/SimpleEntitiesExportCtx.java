package org.echoiot.server.service.sync.vc.data;

import lombok.Getter;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.sync.ie.EntityExportSettings;
import org.echoiot.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.jetbrains.annotations.Nullable;

public class SimpleEntitiesExportCtx extends EntitiesExportCtx<SingleEntityVersionCreateRequest> {

    @Getter
    private final EntityExportSettings settings;

    public SimpleEntitiesExportCtx(User user, CommitGitRequest commit, @Nullable SingleEntityVersionCreateRequest request) {
        this(user, commit, request, request != null ? buildExportSettings(request.getConfig()) : null);
    }

    public SimpleEntitiesExportCtx(User user, CommitGitRequest commit, SingleEntityVersionCreateRequest request, EntityExportSettings settings) {
        super(user, commit, request);
        this.settings = settings;
    }
}
