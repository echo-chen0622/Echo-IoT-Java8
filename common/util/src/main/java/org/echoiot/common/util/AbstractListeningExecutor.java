package org.echoiot.common.util;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;

/**
 * Created by igor on 4/13/18.
 */
public abstract class AbstractListeningExecutor implements ListeningExecutor {

    private ListeningExecutorService service;

    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(EchoiotExecutors.newWorkStealingPool(getThreadPollSize(), getClass()));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    @NotNull
    @Override
    public <T> ListenableFuture<T> executeAsync(@NotNull Callable<T> task) {
        return service.submit(task);
    }

    @NotNull
    public ListenableFuture<?> executeAsync(@NotNull Runnable task) {
        return service.submit(task);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        service.execute(command);
    }

    public ListeningExecutorService executor() {
        return service;
    }

    protected abstract int getThreadPollSize();

}
