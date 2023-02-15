package org.thingsboard.server.queue.kafka;

/**
 * Created by Echo on 25.09.18.
 */
public interface TbKafkaEncoder<T> {

    byte[] encode(T value);

}
