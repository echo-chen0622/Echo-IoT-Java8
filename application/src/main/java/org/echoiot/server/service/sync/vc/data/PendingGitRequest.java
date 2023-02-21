package org.echoiot.server.service.sync.vc.data;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Getter
public class PendingGitRequest<T> {

    private final long createdTime;
    @NotNull
    private final UUID requestId;
    private final TenantId tenantId;
    @NotNull
    private final SettableFuture<T> future;
    @Setter
    private ScheduledFuture<?> timeoutTask;

    public PendingGitRequest(TenantId tenantId) {
        this.createdTime = System.currentTimeMillis();
        this.requestId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.future = SettableFuture.create();
    }

    public boolean requiresSettings() {
        return true;
    }
}
