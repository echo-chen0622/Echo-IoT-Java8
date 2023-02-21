package org.echoiot.server.queue.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.queue.TbQueueAdmin;
import org.echoiot.server.queue.TbQueueCallback;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Echo on 24.09.18.
 */
@Slf4j
public class TbKafkaProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    @NotNull
    private final KafkaProducer<String, byte[]> producer;

    @Getter
    private final String defaultTopic;

    @NotNull
    @Getter
    private final TbKafkaSettings settings;

    private final TbQueueAdmin admin;

    @NotNull
    private final Set<TopicPartitionInfo> topics;

    @Builder
    private TbKafkaProducerTemplate(@NotNull TbKafkaSettings settings, String defaultTopic, String clientId, TbQueueAdmin admin) {
        @NotNull Properties props = settings.toProducerProps();

        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.settings = settings;

        this.producer = new KafkaProducer<>(props);
        this.defaultTopic = defaultTopic;
        this.admin = admin;
        topics = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void init() {
    }

    @Override
    public void send(@NotNull TopicPartitionInfo tpi, @NotNull T msg, @Nullable TbQueueCallback callback) {
        try {
            createTopicIfNotExist(tpi);
            String key = msg.getKey().toString();
            byte[] data = msg.getData();
            ProducerRecord<String, byte[]> record;
            @NotNull Iterable<Header> headers = msg.getHeaders().getData().entrySet().stream().map(e -> new RecordHeader(e.getKey(), e.getValue())).collect(Collectors.toList());
            record = new ProducerRecord<>(tpi.getFullTopicName(), null, key, data, headers);
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    if (callback != null) {
                        callback.onSuccess(new KafkaTbQueueMsgMetadata(metadata));
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(exception);
                    } else {
                        log.warn("Producer template failure: {}", exception.getMessage(), exception);
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            } else {
                log.warn("Producer template failure (send method wrapper): {}", e.getMessage(), e);
            }
            throw e;
        }
    }

    private void createTopicIfNotExist(@NotNull TopicPartitionInfo tpi) {
        if (topics.contains(tpi)) {
            return;
        }
        admin.createTopicIfNotExists(tpi.getFullTopicName());
        topics.add(tpi);
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.close();
        }
    }
}
