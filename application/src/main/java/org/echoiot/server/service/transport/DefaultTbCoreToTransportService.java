package org.echoiot.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.gen.transport.TransportProtos.ToTransportMsg;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.provider.TbQueueProducerProvider;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@TbCoreComponent
public class DefaultTbCoreToTransportService implements TbCoreToTransportService {

    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> tbTransportProducer;

    public DefaultTbCoreToTransportService(NotificationsTopicService notificationsTopicService, TbQueueProducerProvider tbQueueProducerProvider) {
        this.notificationsTopicService = notificationsTopicService;
        this.tbTransportProducer = tbQueueProducerProvider.getTransportNotificationsMsgProducer();
    }

    @Override
    public void process(String nodeId, ToTransportMsg msg) {
        process(nodeId, msg, null, null);
    }

    @Override
    public void process(@Nullable String nodeId, ToTransportMsg msg, @Nullable Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (nodeId == null || nodeId.isEmpty()) {
            log.trace("process: skipping message without nodeId [{}], (ToTransportMsg) msg [{}]", nodeId, msg);
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, nodeId);
        UUID sessionId = new UUID(msg.getSessionIdMSB(), msg.getSessionIdLSB());
        log.trace("[{}][{}] Pushing session data to topic: {}", tpi.getFullTopicName(), sessionId, msg);
        TbProtoQueueMsg<ToTransportMsg> queueMsg = new TbProtoQueueMsg<>(ModelConstants.NULL_UUID, msg);
        tbTransportProducer.send(tpi, queueMsg, new QueueCallbackAdaptor(onSuccess, onFailure));
    }

    private static class QueueCallbackAdaptor implements TbQueueCallback {
        private final Runnable onSuccess;
        private final Consumer<Throwable> onFailure;

        QueueCallbackAdaptor(Runnable onSuccess, Consumer<Throwable> onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (onFailure != null) {
                onFailure.accept(t);
            }
        }
    }
}
