package org.echoiot.server.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.TbQueueMsgHeaders;
import org.echoiot.server.queue.common.DefaultTbQueueMsgHeaders;

import java.util.UUID;

public class KafkaTbQueueMsg implements TbQueueMsg {
    private final UUID key;
    private final TbQueueMsgHeaders headers;
    private final byte[] data;

    public KafkaTbQueueMsg(ConsumerRecord<String, byte[]> record) {
        this.key = UUID.fromString(record.key());
        TbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        record.headers().forEach(header -> {
            headers.put(header.key(), header.value());
        });
        this.headers = headers;
        this.data = record.value();
    }

    @Override
    public UUID getKey() {
        return key;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
