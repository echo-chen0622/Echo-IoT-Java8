package org.echoiot.common.util;

import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public interface ListeningExecutor extends Executor {

    <T> ListenableFuture<T> executeAsync(Callable<T> task);

    default ListenableFuture<?> executeAsync(@NotNull Runnable task) {
        return executeAsync(() -> {
            task.run();
            return null;
        });
    }

    default <T> ListenableFuture<T> submit(Callable<T> task) {
        return executeAsync(task);
    }

    default ListenableFuture<?> submit(@NotNull Runnable task) {
        return executeAsync(task);
    }

}
