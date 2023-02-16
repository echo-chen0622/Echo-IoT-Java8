package org.echoiot.server.common.msg.edge;

import org.echoiot.server.common.msg.aware.TenantAwareMsg;
import org.echoiot.server.common.msg.cluster.ToAllNodesMsg;

public interface EdgeSessionMsg extends TenantAwareMsg, ToAllNodesMsg {
}
