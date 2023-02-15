package org.thingsboard.server.queue.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
@ConfigurationProperties(prefix = "queue.kafka")
@Component
public class TbKafkaSettings {

    @Value("${queue.kafka.bootstrap.servers}")
    private String servers;

    @Value("${queue.kafka.acks}")
    private String acks;

    @Value("${queue.kafka.retries}")
    private int retries;

    @Value("${queue.kafka.compression.type:none}")
    private String compressionType;

    @Value("${queue.kafka.batch.size}")
    private int batchSize;

    @Value("${queue.kafka.linger.ms}")
    private long lingerMs;

    @Value("${queue.kafka.max.request.size:1048576}")
    private int maxRequestSize;

    @Value("${queue.kafka.max.in.flight.requests.per.connection:5}")
    private int maxInFlightRequestsPerConnection;

    @Value("${queue.kafka.buffer.memory}")
    private long bufferMemory;

    @Value("${queue.kafka.replication_factor}")
    @Getter
    private short replicationFactor;

    @Value("${queue.kafka.max_poll_records:8192}")
    private int maxPollRecords;

    @Value("${queue.kafka.max_poll_interval_ms:300000}")
    private int maxPollIntervalMs;

    @Value("${queue.kafka.max_partition_fetch_bytes:16777216}")
    private int maxPartitionFetchBytes;

    @Value("${queue.kafka.fetch_max_bytes:134217728}")
    private int fetchMaxBytes;

    @Value("${queue.kafka.use_confluent_cloud:false}")
    private boolean useConfluent;

    @Value("${queue.kafka.confluent.ssl.algorithm}")
    private String sslAlgorithm;

    @Value("${queue.kafka.confluent.sasl.mechanism}")
    private String saslMechanism;

    @Value("${queue.kafka.confluent.sasl.config}")
    private String saslConfig;

    @Value("${queue.kafka.confluent.security.protocol}")
    private String securityProtocol;

    @Setter
    private List<TbProperty> other;

    @Setter
    private Map<String, List<TbProperty>> consumerPropertiesPerTopic = Collections.emptyMap();

    public Properties toAdminProps() {
        Properties props = toProps();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(AdminClientConfig.RETRIES_CONFIG, retries);

        return props;
    }

    public Properties toConsumerProps(String topic) {
        Properties props = toProps();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        consumerPropertiesPerTopic
                .getOrDefault(topic, Collections.emptyList())
                .forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        return props;
    }

    public Properties toProducerProps() {
        Properties props = toProps();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
        return props;
    }

    private Properties toProps() {
        Properties props = new Properties();

        if (useConfluent) {
            props.put("ssl.endpoint.identification.algorithm", sslAlgorithm);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslConfig);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }

        if (other != null) {
            other.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        }
        return props;
    }

}
