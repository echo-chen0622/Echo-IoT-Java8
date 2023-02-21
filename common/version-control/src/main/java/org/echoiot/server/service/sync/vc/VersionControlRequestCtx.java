package org.echoiot.server.service.sync.vc;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.echoiot.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
@Data
public class VersionControlRequestCtx {
    private final String nodeId;
    @NotNull
    private final UUID requestId;
    @NotNull
    private final TenantId tenantId;
    private final RepositorySettings settings;

    public VersionControlRequestCtx(@NotNull ToVersionControlServiceMsg msg, RepositorySettings settings) {
        this.nodeId = msg.getNodeId();
        this.requestId = new UUID(msg.getRequestIdMSB(), msg.getRequestIdLSB());
        this.tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        this.settings = settings;
    }

    @NotNull
    @Override
    public String toString() {
        return "VersionControlRequestCtx{" +
                "nodeId='" + nodeId + '\'' +
                ", requestId=" + requestId +
                ", tenantId=" + tenantId +
                '}';
    }
}
