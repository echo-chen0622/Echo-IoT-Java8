package org.echoiot.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.queue.discovery.QueueRoutingInfo;
import org.echoiot.server.queue.discovery.QueueRoutingInfoService;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultQueueRoutingInfoService implements QueueRoutingInfoService {

    private final QueueService queueService;

    public DefaultQueueRoutingInfoService(QueueService queueService) {
        this.queueService = queueService;
    }

    @NotNull
    @Override
    public List<QueueRoutingInfo> getAllQueuesRoutingInfo() {
        return queueService.findAllQueues().stream().map(QueueRoutingInfo::new).collect(Collectors.toList());
    }

}
