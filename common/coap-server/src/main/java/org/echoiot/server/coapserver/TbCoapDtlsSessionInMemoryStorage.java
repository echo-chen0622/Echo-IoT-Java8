package org.echoiot.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Data
public class TbCoapDtlsSessionInMemoryStorage {

    private final ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> dtlsSessionsMap = new ConcurrentHashMap<>();
    private long dtlsSessionInactivityTimeout;
    private long dtlsSessionReportTimeout;


    public TbCoapDtlsSessionInMemoryStorage(long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout) {
        this.dtlsSessionInactivityTimeout = dtlsSessionInactivityTimeout;
        this.dtlsSessionReportTimeout = dtlsSessionReportTimeout;
    }

    public void put(InetSocketAddress remotePeer, TbCoapDtlsSessionInfo dtlsSessionInfo) {
        log.trace("DTLS session added to in-memory store: [{}] timestamp: [{}]", remotePeer, dtlsSessionInfo.getLastActivityTime());
        dtlsSessionsMap.putIfAbsent(remotePeer, dtlsSessionInfo);
    }

    public void evictTimeoutSessions() {
        long expTime = System.currentTimeMillis() - dtlsSessionInactivityTimeout;
        dtlsSessionsMap.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastActivityTime() < expTime) {
                log.trace("DTLS session was removed from in-memory store: [{}]", entry.getKey());
                return true;
            } else {
                return false;
            }
        });
    }

}
