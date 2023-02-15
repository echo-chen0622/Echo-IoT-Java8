package org.thingsboard.server.util;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.utils.EventDeduplicationExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class EventDeduplicationExecutorTest {

    ThingsBoardThreadFactory threadFactory = ThingsBoardThreadFactory.forName(getClass().getSimpleName());
    ExecutorService executor;

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void testSimpleFlowSameThread() throws InterruptedException {
        simpleFlow(MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testPeriodicFlowSameThread() throws InterruptedException {
        periodicFlow(MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testExceptionFlowSameThread() throws InterruptedException {
        exceptionFlow(MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testSimpleFlowSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(threadFactory);
        simpleFlow(executor);
    }

    @Test
    public void testPeriodicFlowSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(threadFactory);
        periodicFlow(executor);
    }

    @Test
    public void testExceptionFlowSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(threadFactory);
        exceptionFlow(executor);
    }

    @Test
    public void testSimpleFlowMultiThread() throws InterruptedException {
        executor = Executors.newFixedThreadPool(3, threadFactory);
        simpleFlow(executor);
    }

    @Test
    public void testPeriodicFlowMultiThread() throws InterruptedException {
        executor = Executors.newFixedThreadPool(3, threadFactory);
        periodicFlow(executor);
    }

    @Test
    public void testExceptionFlowMultiThread() throws InterruptedException {
        executor = Executors.newFixedThreadPool(3, threadFactory);
        exceptionFlow(executor);
    }

    private void simpleFlow(ExecutorService executorService) throws InterruptedException {
        try {
            Consumer<String> function = Mockito.spy(StringConsumer.class);
            EventDeduplicationExecutor<String> executor = new EventDeduplicationExecutor<>(EventDeduplicationExecutorTest.class.getSimpleName(), executorService, function);

            String params1 = "params1";
            String params2 = "params2";
            String params3 = "params3";

            executor.submit(params1);
            executor.submit(params2);
            executor.submit(params3);
            Thread.sleep(500);
            Mockito.verify(function).accept(params1);
            Mockito.verify(function).accept(params3);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void periodicFlow(ExecutorService executorService) throws InterruptedException {
        try {
            Consumer<String> function = Mockito.spy(StringConsumer.class);
            EventDeduplicationExecutor<String> executor = new EventDeduplicationExecutor<>(EventDeduplicationExecutorTest.class.getSimpleName(), executorService, function);

            String params1 = "params1";
            String params2 = "params2";
            String params3 = "params3";

            executor.submit(params1);
            Thread.sleep(500);
            executor.submit(params2);
            Thread.sleep(500);
            executor.submit(params3);
            Thread.sleep(500);
            Mockito.verify(function).accept(params1);
            Mockito.verify(function).accept(params2);
            Mockito.verify(function).accept(params3);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void exceptionFlow(ExecutorService executorService) throws InterruptedException {
        try {
            Consumer<String> function = Mockito.spy(StringConsumer.class);
            EventDeduplicationExecutor<String> executor = new EventDeduplicationExecutor<>(EventDeduplicationExecutorTest.class.getSimpleName(), executorService, function);

            String params1 = "params1";
            String params2 = "params2";
            String params3 = "params3";

            Mockito.doThrow(new RuntimeException()).when(function).accept("params1");
            executor.submit(params1);
            executor.submit(params2);
            Thread.sleep(500);
            executor.submit(params3);
            Thread.sleep(500);
            Mockito.verify(function).accept(params2);
            Mockito.verify(function).accept(params3);
        } finally {
            executorService.shutdownNow();
        }
    }

    public static class StringConsumer implements Consumer<String> {
        @Override
        public void accept(String s) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
