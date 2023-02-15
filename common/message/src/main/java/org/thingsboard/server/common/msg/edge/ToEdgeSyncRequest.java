package org.thingsboard.server.common.msg.edge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ToEdgeSyncRequest implements EdgeSessionMsg {
    private final UUID id;
    private final TenantId tenantId;
    private final EdgeId edgeId;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG;
    }
}
