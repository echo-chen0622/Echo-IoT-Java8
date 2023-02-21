package org.echoiot.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.*;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.TbQueueRequestTemplate;
import org.echoiot.server.queue.common.DefaultTbQueueRequestTemplate;
import org.echoiot.server.queue.common.TbProtoJsQueueMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.kafka.*;
import org.echoiot.server.queue.settings.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='monolith'")
public class KafkaMonolithQueueFactory implements TbCoreQueueFactory, TbRuleEngineQueueFactory, TbVersionControlQueueFactory {

    private final NotificationsTopicService notificationsTopicService;
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueVersionControlSettings vcSettings;
    private final TbKafkaConsumerStatsService consumerStatsService;

    @NotNull
    private final TbQueueAdmin coreAdmin;
    @NotNull
    private final TbQueueAdmin ruleEngineAdmin;
    @NotNull
    private final TbQueueAdmin jsExecutorRequestAdmin;
    @NotNull
    private final TbQueueAdmin jsExecutorResponseAdmin;
    @NotNull
    private final TbQueueAdmin transportApiRequestAdmin;
    @NotNull
    private final TbQueueAdmin transportApiResponseAdmin;
    @NotNull
    private final TbQueueAdmin notificationAdmin;
    @NotNull
    private final TbQueueAdmin fwUpdatesAdmin;
    @NotNull
    private final TbQueueAdmin vcAdmin;

    private final AtomicLong consumerCount = new AtomicLong();

    public KafkaMonolithQueueFactory(NotificationsTopicService notificationsTopicService, @NotNull TbKafkaSettings kafkaSettings,
                                     TbServiceInfoProvider serviceInfoProvider,
                                     TbQueueCoreSettings coreSettings,
                                     TbQueueRuleEngineSettings ruleEngineSettings,
                                     TbQueueTransportApiSettings transportApiSettings,
                                     TbQueueTransportNotificationSettings transportNotificationSettings,
                                     TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                     TbQueueVersionControlSettings vcSettings,
                                     TbKafkaConsumerStatsService consumerStatsService,
                                     @NotNull TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.notificationsTopicService = notificationsTopicService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.vcSettings = vcSettings;
        this.consumerStatsService = consumerStatsService;

        this.coreAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.jsExecutorRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorRequestConfigs());
        this.jsExecutorResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorResponseConfigs());
        this.transportApiRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiRequestConfigs());
        this.transportApiResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiResponseConfigs());
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
        this.fwUpdatesAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getFwUpdatesConfigs());
        this.vcAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getVcConfigs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-transport-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(transportNotificationSettings.getNotificationsTopic());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-rule-engine-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(ruleEngineSettings.getTopic());
        requestBuilder.admin(ruleEngineAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-rule-engine-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(ruleEngineSettings.getTopic());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(vcSettings.getTopic());
        consumerBuilder.clientId("monolith-vc-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-vc-node");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(vcAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(@NotNull Queue configuration) {
        String queueName = configuration.getName();
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(configuration.getTopic());
        consumerBuilder.clientId("re-" + queueName + "-consumer-" + serviceInfoProvider.getServiceId() + "-" + consumerCount.incrementAndGet());
        consumerBuilder.groupId("re-" + queueName + "-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(ruleEngineAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("monolith-rule-engine-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-rule-engine-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(coreSettings.getTopic());
        consumerBuilder.clientId("monolith-core-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-core-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(coreAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("monolith-core-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-core-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(transportApiSettings.getRequestsTopic());
        consumerBuilder.clientId("monolith-transport-api-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-transport-api-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(transportApiRequestAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-transport-api-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(transportApiSettings.getResponsesTopic());
        requestBuilder.admin(transportApiResponseAdmin);
        return requestBuilder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-js-invoke-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(jsInvokeSettings.getRequestTopic());
        requestBuilder.admin(jsExecutorRequestAdmin);

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("js-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("rule-engine-node-" + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                }
        );
        responseBuilder.statsService(consumerStatsService);
        responseBuilder.admin(jsExecutorResponseAdmin);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorResponseAdmin);
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(coreSettings.getUsageStatsTopic());
        consumerBuilder.clientId("monolith-us-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-us-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(coreAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(coreSettings.getOtaPackageTopic());
        consumerBuilder.clientId("monolith-ota-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("monolith-ota-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(fwUpdatesAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-ota-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getOtaPackageTopic());
        requestBuilder.admin(fwUpdatesAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-us-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getUsageStatsTopic());
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("monolith-vc-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(vcSettings.getTopic());
        requestBuilder.admin(vcAdmin);
        return requestBuilder.build();
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorRequestAdmin != null) {
            jsExecutorRequestAdmin.destroy();
        }
        if (jsExecutorResponseAdmin != null) {
            jsExecutorResponseAdmin.destroy();
        }
        if (transportApiRequestAdmin != null) {
            transportApiRequestAdmin.destroy();
        }
        if (transportApiResponseAdmin != null) {
            transportApiResponseAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (fwUpdatesAdmin != null) {
            fwUpdatesAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
    }
}
