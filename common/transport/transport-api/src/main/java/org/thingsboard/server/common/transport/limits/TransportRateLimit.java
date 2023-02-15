package org.thingsboard.server.common.transport.limits;

public interface TransportRateLimit {

    String getConfiguration();

    boolean tryConsume();

    boolean tryConsume(long number);

}
