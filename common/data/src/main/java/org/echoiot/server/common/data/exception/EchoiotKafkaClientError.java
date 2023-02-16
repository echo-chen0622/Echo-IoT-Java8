package org.echoiot.server.common.data.exception;

public class EchoiotKafkaClientError extends Error {

    public EchoiotKafkaClientError(String message) {
        super(message);
    }
}
