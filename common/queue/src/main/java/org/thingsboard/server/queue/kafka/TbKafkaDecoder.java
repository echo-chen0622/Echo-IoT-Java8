package org.thingsboard.server.queue.kafka;

import org.thingsboard.server.queue.TbQueueMsg;

import java.io.IOException;

/**
 * Created by ashvayka on 25.09.18.
 */
public interface TbKafkaDecoder<T> {

    T decode(TbQueueMsg msg) throws IOException;

}
