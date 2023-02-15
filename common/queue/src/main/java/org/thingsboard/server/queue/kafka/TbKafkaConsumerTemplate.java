package org.thingsboard.server.queue.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.util.StopWatch;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    private final TbQueueAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;

    private final TbKafkaConsumerStatsService statsService;
    private final String groupId;

    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatsService statsService) {
        super(topic);
        Properties props = settings.toConsumerProps(topic);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.statsService = statsService;
        this.groupId = groupId;

        if (statsService != null) {
            statsService.registerClientGroup(groupId);
        }

        this.admin = admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        if (!topicNames.isEmpty()) {
            topicNames.forEach(admin::createTopicIfNotExists);
            log.info("subscribe topics {}", topicNames);
            consumer.subscribe(topicNames);
        } else {
            log.info("unsubscribe due to empty topic list");
            consumer.unsubscribe();
        }
    }

    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.trace("poll topic {} maxDuration {}", getTopic(), durationInMillis);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));

        stopWatch.stop();
        log.trace("poll topic {} took {}ms", getTopic(), stopWatch.getTotalTimeMillis());

        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ConsumerRecord<String, byte[]>> recordList = new ArrayList<>(256);
            records.forEach(recordList::add);
            return recordList;
        }
    }

    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    @Override
    protected void doCommit() {
        consumer.commitSync();
    }

    @Override
    protected void doUnsubscribe() {
        log.info("unsubscribe topic and close consumer for topic {}", getTopic());
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        if (statsService != null) {
            statsService.unregisterClientGroup(groupId);
        }
    }
}
