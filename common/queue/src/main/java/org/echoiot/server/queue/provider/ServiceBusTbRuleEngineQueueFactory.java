package org.echoiot.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueConsumer;
import org.echoiot.server.queue.TbQueueProducer;
import org.echoiot.server.queue.TbQueueRequestTemplate;
import org.echoiot.server.queue.discovery.NotificationsTopicService;
import org.echoiot.server.queue.discovery.TbServiceInfoProvider;
import org.echoiot.server.queue.settings.TbQueueCoreSettings;
import org.echoiot.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.echoiot.server.queue.settings.TbQueueRuleEngineSettings;
import org.echoiot.server.queue.settings.TbQueueTransportNotificationSettings;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToTransportMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusConsumerTemplate;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusProducerTemplate;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusQueueConfigs;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusSettings;
import org.echoiot.server.queue.common.DefaultTbQueueRequestTemplate;
import org.echoiot.server.queue.common.TbProtoJsQueueMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus' && '${service.type:null}'=='tb-rule-engine'")
public class ServiceBusTbRuleEngineQueueFactory implements TbRuleEngineQueueFactory {

    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbServiceBusSettings serviceBusSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;

    @NotNull
    private final TbQueueAdmin coreAdmin;
    @NotNull
    private final TbQueueAdmin ruleEngineAdmin;
    @NotNull
    private final TbQueueAdmin jsExecutorAdmin;
    @NotNull
    private final TbQueueAdmin notificationAdmin;

    public ServiceBusTbRuleEngineQueueFactory(NotificationsTopicService notificationsTopicService, TbQueueCoreSettings coreSettings,
                                              TbQueueRuleEngineSettings ruleEngineSettings,
                                              TbServiceInfoProvider serviceInfoProvider,
                                              @NotNull TbServiceBusSettings serviceBusSettings,
                                              TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                              TbQueueTransportNotificationSettings transportNotificationSettings,
                                              @NotNull TbServiceBusQueueConfigs serviceBusQueueConfigs) {
        this.notificationsTopicService = notificationsTopicService;
        this.coreSettings = coreSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.ruleEngineSettings = ruleEngineSettings;
        this.serviceBusSettings = serviceBusSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.transportNotificationSettings = transportNotificationSettings;

        this.coreAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
        this.jsExecutorAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getJsExecutorConfigs());
        this.notificationAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getNotificationsConfigs());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbServiceBusProducerTemplate<>(ruleEngineAdmin, serviceBusSettings, ruleEngineSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, ruleEngineSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, coreSettings.getTopic());
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(@NotNull Queue configuration) {
        return new TbServiceBusConsumerTemplate<>(ruleEngineAdmin, serviceBusSettings, configuration.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @NotNull
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        return new TbServiceBusConsumerTemplate<>(notificationAdmin, serviceBusSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        @NotNull TbQueueProducer<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> producer = new TbServiceBusProducerTemplate<>(jsExecutorAdmin, serviceBusSettings, jsInvokeSettings.getRequestTopic());
        @NotNull TbQueueConsumer<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> consumer = new TbServiceBusConsumerTemplate<>(jsExecutorAdmin, serviceBusSettings,
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
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, coreSettings.getUsageStatsTopic());
    }

    @NotNull
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, coreSettings.getOtaPackageTopic());
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }
}
