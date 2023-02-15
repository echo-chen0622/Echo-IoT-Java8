package org.thingsboard.server.common.transport.limits;

public class DummyTransportRateLimit implements TransportRateLimit {

    @Override
    public String getConfiguration() {
        return "";
    }

    @Override
    public boolean tryConsume(long number) {
        return true;
    }

    @Override
    public boolean tryConsume() {
        return true;
    }

}
