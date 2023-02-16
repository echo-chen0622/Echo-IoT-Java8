package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.EdgeConfiguration;
import org.echoiot.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
                if (edge != null) {
                    EdgeConfiguration edgeConfigMsg =
                            edgeMsgConstructor.constructEdgeConfiguration(edge);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setEdgeConfiguration(edgeConfigMsg)
                            .build();
                }
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            switch (actionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    Edge edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (edge == null || customerId.isNullUid()) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customerId, null));
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.ASSIGNED_TO_CUSTOMER, edgeId, null));
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<User> pageData;
                    do {
                        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
                        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                            log.trace("[{}] [{}] user(s) are going to be added to edge.", edge.getId(), pageData.getData().size());
                            for (User user : pageData.getData()) {
                                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null));
                            }
                            if (pageData.hasNext()) {
                                pageLink = pageLink.nextPageLink();
                            }
                        }
                    } while (pageData != null && pageData.hasNext());
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), CustomerId.class);
                    edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (edge == null || customerIdToDelete.isNullUid()) {
                        return Futures.immediateFuture(null);
                    }
                    return Futures.transformAsync(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER, edgeId, null),
                            voids -> saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, customerIdToDelete, null),
                            dbCallbackExecutorService);
                default:
                    return Futures.immediateFuture(null);
            }
        } catch (Exception e) {
            log.error("Exception during processing edge event", e);
            return Futures.immediateFailedFuture(e);
        }
    }
}
