package org.thingsboard.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * This class deduplicate executions of the specified function.
 * Useful in cluster mode, when you get event about partition change multiple times.
 * Assuming that the function execution is expensive, we should execute it immediately when first time event occurs and
 * later, once the processing of first event is done, process last pending task.
 *
 * @param <P> parameters of the function
 */
@Slf4j
public class EventDeduplicationExecutor<P> {
    private final String name;
    private final ExecutorService executor;
    private final Consumer<P> function;
    private P pendingTask;
    private boolean busy;

    public EventDeduplicationExecutor(String name, ExecutorService executor, Consumer<P> function) {
        this.name = name;
        this.executor = executor;
        this.function = function;
    }

    public void submit(P params) {
        log.info("[{}] Going to submit: {}", name, params);
        synchronized (EventDeduplicationExecutor.this) {
            if (!busy) {
                busy = true;
                pendingTask = null;
                try {
                    log.info("[{}] Submitting task: {}", name, params);
                    executor.submit(() -> {
                        try {
                            log.info("[{}] Executing task: {}", name, params);
                            function.accept(params);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process task with parameters: {}", name, params, e);
                            throw e;
                        } finally {
                            unlockAndProcessIfAny();
                        }
                    });
                } catch (Throwable e) {
                    log.warn("[{}] Failed to submit task with parameters: {}", name, params, e);
                    unlockAndProcessIfAny();
                    throw e;
                }
            } else {
                log.info("[{}] Task is already in progress. {} pending task: {}", name, pendingTask == null ? "adding" : "updating", params);
                pendingTask = params;
            }
        }
    }

    private void unlockAndProcessIfAny() {
        synchronized (EventDeduplicationExecutor.this) {
            busy = false;
            if (pendingTask != null) {
                submit(pendingTask);
            }
        }
    }
}
