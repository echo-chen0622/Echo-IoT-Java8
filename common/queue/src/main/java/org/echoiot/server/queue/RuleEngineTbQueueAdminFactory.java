package org.echoiot.server.queue;

import org.echoiot.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusQueueConfigs;
import org.echoiot.server.queue.azure.servicebus.TbServiceBusSettings;
import org.echoiot.server.queue.kafka.TbKafkaAdmin;
import org.echoiot.server.queue.kafka.TbKafkaSettings;
import org.echoiot.server.queue.kafka.TbKafkaTopicConfigs;
import org.echoiot.server.queue.pubsub.TbPubSubAdmin;
import org.echoiot.server.queue.pubsub.TbPubSubSettings;
import org.echoiot.server.queue.pubsub.TbPubSubSubscriptionSettings;
import org.echoiot.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.echoiot.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.echoiot.server.queue.rabbitmq.TbRabbitMqSettings;
import org.echoiot.server.queue.sqs.TbAwsSqsAdmin;
import org.echoiot.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.echoiot.server.queue.sqs.TbAwsSqsSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineTbQueueAdminFactory {

    @Autowired(required = false)
    private TbKafkaTopicConfigs kafkaTopicConfigs;
    @Autowired(required = false)
    private TbKafkaSettings kafkaSettings;

    @Autowired(required = false)
    private TbAwsSqsQueueAttributes awsSqsQueueAttributes;
    @Autowired(required = false)
    private TbAwsSqsSettings awsSqsSettings;

    @Autowired(required = false)
    private TbPubSubSubscriptionSettings pubSubSubscriptionSettings;
    @Autowired(required = false)
    private TbPubSubSettings pubSubSettings;

    @Autowired(required = false)
    private TbRabbitMqQueueArguments rabbitMqQueueArguments;
    @Autowired(required = false)
    private TbRabbitMqSettings rabbitMqSettings;

    @Autowired(required = false)
    private TbServiceBusQueueConfigs serviceBusQueueConfigs;
    @Autowired(required = false)
    private TbServiceBusSettings serviceBusSettings;

    @ConditionalOnExpression("'${queue.type:null}'=='kafka'")
    @Bean
    public TbQueueAdmin createKafkaAdmin() {
        return new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
    @Bean
    public TbQueueAdmin createAwsSqsAdmin() {
        return new TbAwsSqsAdmin(awsSqsSettings, awsSqsQueueAttributes.getRuleEngineAttributes());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
    @Bean
    public TbQueueAdmin createPubSubAdmin() {
        return new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getRuleEngineSettings());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
    @Bean
    public TbQueueAdmin createRabbitMqAdmin() {
        return new TbRabbitMqAdmin(rabbitMqSettings, rabbitMqQueueArguments.getRuleEngineArgs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
    @Bean
    public TbQueueAdmin createServiceBusAdmin() {
        return new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
    @Bean
    public TbQueueAdmin createInMemoryAdmin() {
        return new TbQueueAdmin() {

            @Override
            public void createTopicIfNotExists(String topic) {
            }

            @Override
            public void deleteTopic(String topic) {
            }

            @Override
            public void destroy() {
            }
        };
    }
}
