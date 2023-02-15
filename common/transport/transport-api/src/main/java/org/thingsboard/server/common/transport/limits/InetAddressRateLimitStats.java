package org.thingsboard.server.common.transport.limits;

import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class InetAddressRateLimitStats {

    private final Lock lock = new ReentrantLock();

    private boolean blocked;
    private long lastActivityTs;
    private int failureCount;
    private int connectionsCount;

}
