package org.echoiot.server.queue.provider;

import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.rabbitmq.*;
import org.echoiot.server.queue.settings.TbQueueCoreSettings;
import org.echoiot.server.queue.settings.TbQueueVersionControlSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq' && '${service.type:null}'=='tb-vc-executor'")
public class RabbitMqTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    private final TbRabbitMqSettings rabbitMqSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin vcAdmin;

    public RabbitMqTbVersionControlQueueFactory(TbRabbitMqSettings rabbitMqSettings,
                                                TbQueueCoreSettings coreSettings,
                                                TbQueueVersionControlSettings vcSettings,
                                                TbRabbitMqQueueArguments queueArguments
    ) {
        this.rabbitMqSettings = rabbitMqSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getCoreArgs());
        this.notificationAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getNotificationsArgs());
        this.vcAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getVcArgs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(notificationAdmin, rabbitMqSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(vcAdmin, rabbitMqSettings, vcSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
    }
}
