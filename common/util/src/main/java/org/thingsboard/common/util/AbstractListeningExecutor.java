package org.thingsboard.common.util;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.common.util.ListeningExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Created by igor on 4/13/18.
 */
public abstract class AbstractListeningExecutor implements ListeningExecutor {

    private ListeningExecutorService service;

    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(getThreadPollSize(), getClass()));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        return service.submit(task);
    }

    public ListenableFuture<?> executeAsync(Runnable task) {
        return service.submit(task);
    }

    @Override
    public void execute(Runnable command) {
        service.execute(command);
    }

    public ListeningExecutorService executor() {
        return service;
    }

    protected abstract int getThreadPollSize();

}
