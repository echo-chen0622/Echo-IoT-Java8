package org.echoiot.server.queue.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.common.util.TbStopWatch;
import org.echoiot.server.common.msg.queue.TopicPartitionInfo;
import org.echoiot.server.common.stats.MessagesStats;
import org.echoiot.server.queue.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class DefaultTbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueRequestTemplate<Request, Response> {

    private final TbQueueAdmin queueAdmin;
    private final TbQueueProducer<Request> requestTemplate;
    private final TbQueueConsumer<Response> responseTemplate;
    final ConcurrentHashMap<UUID, DefaultTbQueueRequestTemplate.ResponseMetaData<Response>> pendingRequests = new ConcurrentHashMap<>();
    final boolean internalExecutor;
    final ExecutorService executor;
    final long maxRequestTimeoutNs;
    final long maxRequestTimeout;
    final long maxPendingRequests;
    final long pollInterval;
    volatile boolean stopped = false;
    long nextCleanupNs = 0L;
    private final Lock cleanerLock = new ReentrantLock();

    private MessagesStats messagesStats;

    @Builder
    public DefaultTbQueueRequestTemplate(TbQueueAdmin queueAdmin,
                                         TbQueueProducer<Request> requestTemplate,
                                         TbQueueConsumer<Response> responseTemplate,
                                         long maxRequestTimeout,
                                         long maxPendingRequests,
                                         long pollInterval,
                                         @Nullable ExecutorService executor) {
        this.queueAdmin = queueAdmin;
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.maxRequestTimeoutNs = TimeUnit.MILLISECONDS.toNanos(maxRequestTimeout);
        this.maxRequestTimeout = maxRequestTimeout;
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.internalExecutor = (executor == null);
        this.executor = internalExecutor ? createExecutor() : executor;
    }

    @NotNull
    ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("tb-queue-request-template-" + responseTemplate.getTopic()));
    }

    @Override
    public void init() {
        queueAdmin.createTopicIfNotExists(responseTemplate.getTopic());
        requestTemplate.init();
        responseTemplate.subscribe();
        executor.submit(this::mainLoop);
    }

    void mainLoop() {
        while (!stopped) {
            @NotNull TbStopWatch sw = TbStopWatch.create();
            try {
                fetchAndProcessResponses();
            } catch (Throwable e) {
                long sleepNanos = TimeUnit.MILLISECONDS.toNanos(this.pollInterval) - sw.stopAndGetTotalTimeNanos();
                log.warn("Failed to obtain and process responses from queue. Going to sleep " + sleepNanos + "ns", e);
                sleep(sleepNanos);
            }
        }
    }

    void fetchAndProcessResponses() {
        final long pendingRequestsCount = pendingRequests.mappingCount();
        log.trace("Starting template pool topic {}, for pendingRequests {}", responseTemplate.getTopic(), pendingRequestsCount);
        List<Response> responses = doPoll(); //poll js responses
        log.trace("Completed template poll topic {}, for pendingRequests [{}], received [{}] responses", responseTemplate.getTopic(), pendingRequestsCount, responses.size());
        responses.forEach(this::processResponse); //this can take a long time
        responseTemplate.commit();
        tryCleanStaleRequests();
    }

    private boolean tryCleanStaleRequests() {
        if (!cleanerLock.tryLock()) {
            return false;
        }
        try {
            log.trace("tryCleanStaleRequest...");
            final long currentNs = getCurrentClockNs();
            if (nextCleanupNs < currentNs) {
                pendingRequests.forEach((key, value) -> {
                    if (value.expTime < currentNs) {
                        ResponseMetaData<Response> staleRequest = pendingRequests.remove(key);
                        if (staleRequest != null) {
                            setTimeoutException(key, staleRequest, currentNs);
                        }
                    }
                });
                setupNextCleanup();
            }
        } finally {
            cleanerLock.unlock();
        }
        return true;
    }

    void setupNextCleanup() {
        nextCleanupNs = getCurrentClockNs() + maxRequestTimeoutNs;
        log.trace("setupNextCleanup {}", nextCleanupNs);
    }

    List<Response> doPoll() {
        return responseTemplate.poll(pollInterval);
    }

    void sleep(long nanos) {
        LockSupport.parkNanos(nanos);
    }

    void setTimeoutException(UUID key, @NotNull ResponseMetaData<Response> staleRequest, long currentNs) {
        if (currentNs >= staleRequest.getSubmitTime() + staleRequest.getTimeout()) {
            log.debug("Request timeout detected, currentNs [{}], {}, key [{}]", currentNs, staleRequest, key);
        } else {
            log.info("Request timeout detected, currentNs [{}], {}, key [{}]", currentNs, staleRequest, key);
        }
        staleRequest.future.setException(new TimeoutException());
    }

    void processResponse(@NotNull Response response) {
        byte[] requestIdHeader = response.getHeaders().get(REQUEST_ID_HEADER);
        UUID requestId;
        if (requestIdHeader == null) {
            log.error("[{}] Missing requestId in header and body", response);
        } else {
            requestId = bytesToUuid(requestIdHeader);
            log.trace("[{}] Response received: {}", requestId, response);
            ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
            if (expectedResponse == null) {
                log.debug("[{}] Invalid or stale request, response: {}", requestId, String.valueOf(response).replace("\n", " "));
            } else {
                expectedResponse.future.set(response);
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;

        if (responseTemplate != null) {
            responseTemplate.unsubscribe();
        }

        if (requestTemplate != null) {
            requestTemplate.stop();
        }

        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    @Override
    public void setMessagesStats(MessagesStats messagesStats) {
        this.messagesStats = messagesStats;
    }

    @Override
    public ListenableFuture<Response> send(@NotNull Request request) {
        return send(request, this.maxRequestTimeoutNs);
    }

    @NotNull
    @Override
    public ListenableFuture<Response> send(@NotNull Request request, long requestTimeoutNs) {
        if (pendingRequests.mappingCount() >= maxPendingRequests) {
            log.warn("Pending request map is full [{}]! Consider to increase maxPendingRequests or increase processing performance. Request is {}", maxPendingRequests, request);
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        @NotNull UUID requestId = UUID.randomUUID();
        request.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        request.getHeaders().put(RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic()));
        request.getHeaders().put(EXPIRE_TS_HEADER, longToBytes(getCurrentTimeMs() + maxRequestTimeout));
        long currentClockNs = getCurrentClockNs();
        @NotNull SettableFuture<Response> future = SettableFuture.create();
        @NotNull ResponseMetaData<Response> responseMetaData = new ResponseMetaData<>(currentClockNs + requestTimeoutNs, future, currentClockNs, requestTimeoutNs);
        log.trace("pending {}", responseMetaData);
        if (pendingRequests.putIfAbsent(requestId, responseMetaData) != null) {
            log.warn("Pending request already exists [{}]!", maxPendingRequests);
            return Futures.immediateFailedFuture(new RuntimeException("Pending request already exists !" + requestId));
        }
        sendToRequestTemplate(request, requestId, future, responseMetaData);
        return future;
    }

    /**
     * MONOTONIC clock instead jumping wall clock.
     * Wrapped into the method for the test purposes to travel through the time
     * */
    long getCurrentClockNs() {
        return System.nanoTime();
    }

    /**
     * Wall clock to send timestamp to an external service
     * */
    long getCurrentTimeMs() {
        return System.currentTimeMillis();
    }

    void sendToRequestTemplate(@NotNull Request request, @NotNull UUID requestId, @NotNull SettableFuture<Response> future, @NotNull ResponseMetaData<Response> responseMetaData) {
        log.trace("[{}] Sending request, key [{}], expTime [{}], request {}", requestId, request.getKey(), responseMetaData.expTime, request);
        if (messagesStats != null) {
            messagesStats.incrementTotal();
        }
        requestTemplate.send(TopicPartitionInfo.builder().topic(requestTemplate.getDefaultTopic()).build(), request, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                if (messagesStats != null) {
                    messagesStats.incrementSuccessful();
                }
                log.trace("[{}] Request sent: {}, request {}", requestId, metadata, request);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                if (messagesStats != null) {
                    messagesStats.incrementFailed();
                }
                pendingRequests.remove(requestId);
                future.setException(t);
            }
        });
    }

    @Getter
    static class ResponseMetaData<T> {
        private final long submitTime;
        private final long timeout;
        private final long expTime;
        private final SettableFuture<T> future;

        ResponseMetaData(long ts, SettableFuture<T> future, long submitTime, long timeout) {
            this.submitTime = submitTime;
            this.timeout = timeout;
            this.expTime = ts;
            this.future = future;
        }

        @NotNull
        @Override
        public String toString() {
            return "ResponseMetaData{" +
                    "submitTime=" + submitTime +
                    ", calculatedExpTime=" + (submitTime + timeout) +
                    ", deltaMs=" + (expTime - submitTime) +
                    ", expTime=" + expTime +
                    ", future=" + future +
                    '}';
        }
    }

}
