package org.echoiot.server.service.edge.rpc;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Data
public class EdgeSessionState {

    private final Map<Integer, DownlinkMsg> pendingMsgsMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private SettableFuture<Void> sendDownlinkMsgsFuture;
    private ScheduledFuture<?> scheduledSendDownlinkTask;
}
