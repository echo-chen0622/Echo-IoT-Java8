package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.QueueUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;

@Component
@Slf4j
@TbCoreComponent
public class QueueEdgeProcessor extends BaseEdgeProcessor {

    @Nullable
    public DownlinkMsg convertQueueEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull QueueId queueId = new QueueId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                Queue queue = queueService.findQueueById(edgeEvent.getTenantId(), queueId);
                if (queue != null) {
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    QueueUpdateMsg queueUpdateMsg =
                            queueMsgConstructor.constructQueueUpdatedMsg(msgType, queue);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addQueueUpdateMsg(queueUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                QueueUpdateMsg queueDeleteMsg =
                        queueMsgConstructor.constructQueueDeleteMsg(queueId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addQueueUpdateMsg(queueDeleteMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processQueueNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }
}
