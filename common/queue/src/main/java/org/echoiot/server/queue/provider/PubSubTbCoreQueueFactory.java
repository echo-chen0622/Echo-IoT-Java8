package org.echoiot.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.TbQueueRequestTemplate;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToTransportMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.echoiot.server.queue.common.DefaultTbQueueRequestTemplate;
import org.echoiot.server.queue.common.TbProtoJsQueueMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.pubsub.TbPubSubAdmin;
import org.echoiot.server.queue.pubsub.TbPubSubConsumerTemplate;
import org.echoiot.server.queue.pubsub.TbPubSubProducerTemplate;
import org.echoiot.server.queue.pubsub.TbPubSubSettings;
import org.echoiot.server.queue.pubsub.TbPubSubSubscriptionSettings;
import org.echoiot.server.queue.settings.TbQueueCoreSettings;
import org.echoiot.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.echoiot.server.queue.settings.TbQueueRuleEngineSettings;
import org.echoiot.server.queue.settings.TbQueueTransportApiSettings;
import org.echoiot.server.queue.settings.TbQueueTransportNotificationSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub' && '${service.type:null}'=='tb-core'")
public class PubSubTbCoreQueueFactory implements TbCoreQueueFactory {

    private final TbPubSubSettings pubSubSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final NotificationsTopicService notificationsTopicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;

    @NotNull
    private final TbQueueAdmin coreAdmin;
    @NotNull
    private final TbQueueAdmin jsExecutorAdmin;
    @NotNull
    private final TbQueueAdmin transportApiAdmin;
    @NotNull
    private final TbQueueAdmin notificationAdmin;
    @NotNull
    private final TbQueueAdmin ruleEngineAdmin;

    public PubSubTbCoreQueueFactory(@NotNull TbPubSubSettings pubSubSettings,
                                    TbQueueCoreSettings coreSettings,
                                    TbQueueTransportApiSettings transportApiSettings,
                                    NotificationsTopicService notificationsTopicService,
                                    TbServiceInfoProvider serviceInfoProvider,
                                    TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                    TbQueueTransportNotificationSettings transportNotificationSettings,
                                    TbQueueRuleEngineSettings ruleEngineSettings,
                                    @NotNull TbPubSubSubscriptionSettings pubSubSubscriptionSettings) {
        this.pubSubSettings = pubSubSettings;
        this.coreSettings = coreSettings;
        this.transportApiSettings = transportApiSettings;
        this.notificationsTopicService = notificationsTopicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.jsInvokeSettings = jsInvokeSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.ruleEngineSettings = ruleEngineSettings;

        this.coreAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getCoreSettings());
        this.jsExecutorAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getJsExecutorSettings());
        this.transportApiAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getTransportApiSettings());
        this.notificationAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getNotificationsSettings());
        this.ruleEngineAdmin = new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getRuleEngineSettings());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(notificationAdmin, pubSubSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(notificationAdmin, pubSubSettings, ruleEngineSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(notificationAdmin, pubSubSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(notificationAdmin, pubSubSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbPubSubConsumerTemplate<>(transportApiAdmin, pubSubSettings, transportApiSettings.getRequestsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbPubSubProducerTemplate<>(transportApiAdmin, pubSubSettings, transportApiSettings.getResponsesTopic());
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        @NotNull TbQueueProducer<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> producer = new TbPubSubProducerTemplate<>(jsExecutorAdmin, pubSubSettings, jsInvokeSettings.getRequestTopic());
        @NotNull TbQueueConsumer<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> consumer = new TbPubSubConsumerTemplate<>(jsExecutorAdmin, pubSubSettings,
                jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                });

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getUsageStatsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getOtaPackageTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getOtaPackageTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbPubSubProducerTemplate<>(coreAdmin, pubSubSettings, coreSettings.getUsageStatsTopic());
    }

    @Nullable
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        //TODO: version-control
        return null;
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
    }
}
