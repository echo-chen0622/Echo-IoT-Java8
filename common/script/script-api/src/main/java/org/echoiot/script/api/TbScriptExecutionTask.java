package org.echoiot.script.api;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;


@RequiredArgsConstructor
public abstract class TbScriptExecutionTask {

    @NotNull
    @Getter
    private final ListenableFuture<Object> resultFuture;

    public abstract void stop();
}
