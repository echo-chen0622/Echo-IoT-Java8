package org.thingsboard.server.queue.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

import javax.annotation.PreDestroy;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='tb-vc-executor'")
public class AwsSqsTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;


    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin vcAdmin;

    public AwsSqsTbVersionControlQueueFactory(TbAwsSqsSettings sqsSettings,
                                              TbQueueCoreSettings coreSettings,
                                              TbQueueVersionControlSettings vcSettings,
                                              TbAwsSqsQueueAttributes sqsQueueAttributes
    ) {
        this.sqsSettings = sqsSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.vcAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getVcAttributes());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, coreSettings.getTopic());
    }

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
