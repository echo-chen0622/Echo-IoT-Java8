package org.thingsboard.server.common.data.exception;

public class ThingsboardKafkaClientError extends Error {

    public ThingsboardKafkaClientError(String message) {
        super(message);
    }
}
