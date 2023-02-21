package org.echoiot.common.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class DonAsynchron {

    public static <T> void withCallback(@NotNull ListenableFuture<T> future, @NotNull Consumer<T> onSuccess,
                                        @NotNull Consumer<Throwable> onFailure) {
        withCallback(future, onSuccess, onFailure, null);
    }

    public static <T> void withCallback(@NotNull ListenableFuture<T> future, @NotNull Consumer<T> onSuccess,
                                        @NotNull Consumer<Throwable> onFailure, @Nullable Executor executor) {
        @NotNull FutureCallback<T> callback = new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onFailure.accept(t);
            }
        };
        if (executor != null) {
            Futures.addCallback(future, callback, executor);
        } else {
            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    @NotNull
    public static <T> ListenableFuture<T> submit(@NotNull Callable<T> task, @NotNull Consumer<T> onSuccess, @NotNull Consumer<Throwable> onFailure, @NotNull Executor executor) {
        return submit(task, onSuccess, onFailure, executor, null);
    }

    @NotNull
    public static <T> ListenableFuture<T> submit(@NotNull Callable<T> task, @NotNull Consumer<T> onSuccess, @NotNull Consumer<Throwable> onFailure, @NotNull Executor executor, Executor callbackExecutor) {
        @NotNull ListenableFuture<T> future = Futures.submit(task, executor);
        withCallback(future, onSuccess, onFailure, callbackExecutor);
        return future;
    }

}
