package org.echoiot.server.controller;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;

import javax.annotation.Resource;
import java.util.UUID;

public class AutoCommitController extends BaseController {

    @Resource
    private EntitiesVersionControlService vcService;

    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }


}
