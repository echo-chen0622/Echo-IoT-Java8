package org.echoiot.server.queue.provider;

import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.settings.TbQueueCoreSettings;
import org.echoiot.server.queue.settings.TbQueueVersionControlSettings;
import org.echoiot.server.queue.sqs.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='tb-vc-executor'")
public class AwsSqsTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;


    @NotNull
    private final TbQueueAdmin coreAdmin;
    @NotNull
    private final TbQueueAdmin notificationAdmin;
    @NotNull
    private final TbQueueAdmin vcAdmin;

    public AwsSqsTbVersionControlQueueFactory(@NotNull TbAwsSqsSettings sqsSettings,
                                              TbQueueCoreSettings coreSettings,
                                              TbQueueVersionControlSettings vcSettings,
                                              @NotNull TbAwsSqsQueueAttributes sqsQueueAttributes
    ) {
        this.sqsSettings = sqsSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.vcAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getVcAttributes());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, coreSettings.getUsageStatsTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(vcAdmin, sqsSettings, vcSettings.getTopic(),
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
