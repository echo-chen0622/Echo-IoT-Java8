package org.echoiot.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPackProcessingContextTest {

    public static final int TIMEOUT = 10;
    ExecutorService executorService;

    @After
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testHighConcurrencyCase() throws InterruptedException {
        //log.warn("preparing the test...");
        int msgCount = 1000;
        int parallelCount = 5;
        executorService = Executors.newFixedThreadPool(parallelCount, EchoiotThreadFactory.forName(getClass().getSimpleName() + "-test-scope"));

        ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> messages = new ConcurrentHashMap<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            messages.put(UUID.randomUUID(), new TbProtoQueueMsg<>(UUID.randomUUID(), null));
        }
        TbRuleEngineSubmitStrategy strategyMock = mock(TbRuleEngineSubmitStrategy.class);
        when(strategyMock.getPendingMap()).thenReturn(messages);

        TbMsgPackProcessingContext context = new TbMsgPackProcessingContext(DataConstants.MAIN_QUEUE_NAME, strategyMock, false);
        for (UUID uuid : messages.keySet()) {
            final CountDownLatch readyLatch = new CountDownLatch(parallelCount);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(parallelCount);
            for (int i = 0; i < parallelCount; i++) {
                //final String taskName = "" + uuid + " " + i;
                executorService.submit(() -> {
                    //log.warn("ready {}", taskName);
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Assert.fail("failed to await");
                    }
                    //log.warn("go    {}", taskName);

                    context.onSuccess(uuid);

                    finishLatch.countDown();
                });
            }
            assertTrue(readyLatch.await(TIMEOUT, TimeUnit.SECONDS));
            Thread.yield();
            startLatch.countDown(); //run all-at-once submitted tasks
            assertTrue(finishLatch.await(TIMEOUT, TimeUnit.SECONDS));
        }
        assertTrue(context.await(TIMEOUT, TimeUnit.SECONDS));
        verify(strategyMock, times(msgCount)).onSuccess(any(UUID.class));
    }
}
