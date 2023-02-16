package org.echoiot.server.queue.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.echoiot.server.queue.TbQueueMsgMetadata;

@Data
@AllArgsConstructor
public class KafkaTbQueueMsgMetadata implements TbQueueMsgMetadata {
    private RecordMetadata metadata;
}
