package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.ToString;

@ToString
public class ProcessFailureStrategy {

    @Getter
    private boolean stop;

    private ProcessFailureStrategy(boolean stop) {
        this.stop = stop;
    }

    public static ProcessFailureStrategy stop() {
        return new ProcessFailureStrategy(true);
    }

    public static ProcessFailureStrategy resume() {
        return new ProcessFailureStrategy(false);
    }
}
