package org.echoiot.server.queue.provider;

import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.pubsub.*;
import org.echoiot.server.queue.settings.TbQueueCoreSettings;
import org.echoiot.server.queue.settings.TbQueueVersionControlSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub' && '${service.type:null}'=='tb-vc-executor'")
public class PubSubTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    private final TbPubSubSettings pubSubSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin vcAdmin;

    public PubSubTbVersionControlQueueFactory(TbPubSubSettings pubSubSettings,
                                              TbQueueCoreSettings coreSettings,
                                              TbQueueVersionControlSettings vcSettings,
                                              TbPubSubSubscriptionSettings pubSubSubscriptionSettings
    ) {
        this.pubSubSettings = pubSubSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getCoreSettings());
        this.notificationAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getNotificationsSettings());
        this.vcAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getVcSettings());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(notificationAdmin, pubSubSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(vcAdmin, pubSubSettings, vcSettings.getTopic(),
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
