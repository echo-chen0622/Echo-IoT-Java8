package org.thingsboard.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class TbApplicationEventListener<T extends TbApplicationEvent> implements ApplicationListener<T> {

    private int lastProcessedSequenceNumber = Integer.MIN_VALUE;
    private final Lock seqNumberLock = new ReentrantLock();

    @Override
    public void onApplicationEvent(T event) {
        boolean validUpdate = false;
        seqNumberLock.lock();
        try {
            if (event.getSequenceNumber() > lastProcessedSequenceNumber) {
                validUpdate = true;
                lastProcessedSequenceNumber = event.getSequenceNumber();
            }
        } finally {
            seqNumberLock.unlock();
        }
        if (validUpdate && filterTbApplicationEvent(event)) {
            onTbApplicationEvent(event);
        } else {
            log.info("Application event ignored due to invalid sequence number ({} > {}). Event: {}", lastProcessedSequenceNumber, event.getSequenceNumber(), event);
        }
    }

    protected abstract void onTbApplicationEvent(T event);

    protected boolean filterTbApplicationEvent(T event) {
        return true;
    }

}
