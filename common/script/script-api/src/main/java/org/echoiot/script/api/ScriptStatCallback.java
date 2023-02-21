package org.echoiot.script.api;

import com.google.common.util.concurrent.FutureCallback;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
public class ScriptStatCallback<T> implements FutureCallback<T> {

    @NotNull
    private final AtomicInteger successMsgs;
    @NotNull
    private final AtomicInteger timeoutMsgs;
    @NotNull
    private final AtomicInteger failedMsgs;

    @Override
    public void onSuccess(@Nullable T result) {
        successMsgs.incrementAndGet();
    }

    @Override
    public void onFailure(Throwable t) {
        if (t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
            timeoutMsgs.incrementAndGet();
        } else {
            failedMsgs.incrementAndGet();
        }
    }
}
