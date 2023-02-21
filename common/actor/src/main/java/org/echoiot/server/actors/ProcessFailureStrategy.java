package org.echoiot.server.actors;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class ProcessFailureStrategy {

    @Getter
    private final boolean stop;

    private ProcessFailureStrategy(boolean stop) {
        this.stop = stop;
    }

    @NotNull
    public static ProcessFailureStrategy stop() {
        return new ProcessFailureStrategy(true);
    }

    @NotNull
    public static ProcessFailureStrategy resume() {
        return new ProcessFailureStrategy(false);
    }
}
