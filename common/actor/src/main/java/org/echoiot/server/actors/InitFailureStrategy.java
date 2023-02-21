package org.echoiot.server.actors;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public static InitFailureStrategy retryImmediately() {
        return new InitFailureStrategy(false, 0);
    }

    @NotNull
    public static InitFailureStrategy retryWithDelay(long ms) {
        return new InitFailureStrategy(false, ms);
    }

    @NotNull
    public static InitFailureStrategy stop() {
        return new InitFailureStrategy(true, 0);
    }
}
