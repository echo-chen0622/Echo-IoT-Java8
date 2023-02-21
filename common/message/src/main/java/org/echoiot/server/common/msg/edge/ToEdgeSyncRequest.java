package org.echoiot.server.common.msg.edge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ToEdgeSyncRequest implements EdgeSessionMsg {
    @NotNull
    private final UUID id;
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final EdgeId edgeId;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG;
    }
}
