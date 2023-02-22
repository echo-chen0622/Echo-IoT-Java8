package org.echoiot.server.actors;

import lombok.Getter;
import lombok.ToString;

@ToString
public class InitFailureStrategy {

    @Getter
    private final boolean stop;
    @Getter
    private final long retryDelay;

    private InitFailureStrategy(boolean stop, long retryDelay) {
        this.stop = stop;
        this.retryDelay = retryDelay;
    }

    public static InitFailureStrategy retryImmediately() {
        return new InitFailureStrategy(false, 0);
    }

    public static InitFailureStrategy retryWithDelay(long ms) {
        return new InitFailureStrategy(false, ms);
    }

    public static InitFailureStrategy stop() {
        return new InitFailureStrategy(true, 0);
    }
}
