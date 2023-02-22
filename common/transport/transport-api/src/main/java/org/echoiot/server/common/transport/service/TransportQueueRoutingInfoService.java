package org.echoiot.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.gen.transport.TransportProtos.GetAllQueueRoutingInfoRequestMsg;
import org.echoiot.server.queue.discovery.QueueRoutingInfo;
import org.echoiot.server.queue.discovery.QueueRoutingInfoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport'")
public class TransportQueueRoutingInfoService implements QueueRoutingInfoService {

    private final TransportService transportService;

    public TransportQueueRoutingInfoService(@Lazy TransportService transportService) {
        this.transportService = transportService;
    }

    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        GetAllQueueRoutingInfoRequestMsg msg = GetAllQueueRoutingInfoRequestMsg.newBuilder().build();
        return transportService.getQueueRoutingInfo(msg).stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }
}
