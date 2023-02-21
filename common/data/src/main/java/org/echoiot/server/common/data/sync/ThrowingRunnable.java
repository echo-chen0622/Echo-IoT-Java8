package org.echoiot.server.common.data.sync;

import org.echoiot.server.common.data.exception.EchoiotException;
import org.jetbrains.annotations.NotNull;

public interface ThrowingRunnable {

    void run() throws EchoiotException;

    @NotNull
    default ThrowingRunnable andThen(@NotNull ThrowingRunnable after) {
        return () -> {
            this.run();
            after.run();
        };
    }

}
