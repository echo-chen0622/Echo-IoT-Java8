package org.thingsboard.server.common.msg.edge;

import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;

import java.io.Serializable;

public interface EdgeSessionMsg extends TenantAwareMsg, ToAllNodesMsg {
}
