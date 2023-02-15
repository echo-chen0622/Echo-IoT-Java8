package org.thingsboard.server.common.msg.edge;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

@ToString
public class EdgeEventUpdateMsg implements EdgeSessionMsg {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EdgeId edgeId;

    public EdgeEventUpdateMsg(TenantId tenantId, EdgeId edgeId) {
        this.tenantId = tenantId;
        this.edgeId = edgeId;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG;
    }
}
