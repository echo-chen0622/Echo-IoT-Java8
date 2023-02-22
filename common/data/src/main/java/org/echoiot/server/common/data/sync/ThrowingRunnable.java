package org.echoiot.server.common.data.sync;

import org.echoiot.server.common.data.exception.EchoiotException;

public interface ThrowingRunnable {

    void run() throws EchoiotException;

    default ThrowingRunnable andThen(ThrowingRunnable after) {
        return () -> {
            this.run();
            after.run();
        };
    }

}
