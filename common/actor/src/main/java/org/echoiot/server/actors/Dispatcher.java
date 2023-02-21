package org.echoiot.server.actors;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

@Data
class Dispatcher {

    @NotNull
    private final String dispatcherId;
    @NotNull
    private final ExecutorService executor;

}
