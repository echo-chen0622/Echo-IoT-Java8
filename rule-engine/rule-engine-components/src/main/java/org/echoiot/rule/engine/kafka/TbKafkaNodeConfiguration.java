package org.echoiot.rule.engine.kafka;

import lombok.Data;
import org.apache.kafka.common.serialization.StringSerializer;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

@Data
public class TbKafkaNodeConfiguration implements NodeConfiguration<TbKafkaNodeConfiguration> {

    private String topicPattern;
    private String keyPattern;
    private String bootstrapServers;
    private int retries;
    private int batchSize;
    private int linger;
    private int bufferMemory;
    private String acks;
    private String keySerializer;
    private String valueSerializer;
    private Map<String, String> otherProperties;

    private boolean addMetadataKeyValuesAsKafkaHeaders;
    private String kafkaHeadersCharset;

    @NotNull
    @Override
    public TbKafkaNodeConfiguration defaultConfiguration() {
        @NotNull TbKafkaNodeConfiguration configuration = new TbKafkaNodeConfiguration();
        configuration.setTopicPattern("my-topic");
        configuration.setBootstrapServers("localhost:9092");
        configuration.setRetries(0);
        configuration.setBatchSize(16384);
        configuration.setLinger(0);
        configuration.setBufferMemory(33554432);
        configuration.setAcks("-1");
        configuration.setKeySerializer(StringSerializer.class.getName());
        configuration.setValueSerializer(StringSerializer.class.getName());
        configuration.setOtherProperties(Collections.emptyMap());
        configuration.setAddMetadataKeyValuesAsKafkaHeaders(false);
        configuration.setKafkaHeadersCharset("UTF-8");
        return configuration;
    }
}
