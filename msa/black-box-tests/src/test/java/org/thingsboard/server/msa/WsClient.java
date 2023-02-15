package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WsClient extends WebSocketClient {
    private static final ObjectMapper mapper = new ObjectMapper();
    private WsTelemetryResponse message;

    private volatile boolean firstReplyReceived;
    private CountDownLatch firstReply = new CountDownLatch(1);
    private CountDownLatch latch = new CountDownLatch(1);

    private final long timeoutMultiplier;

    WsClient(URI serverUri, long timeoutMultiplier) {
        super(serverUri);
        this.timeoutMultiplier = timeoutMultiplier;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String message) {
        if (!firstReplyReceived) {
            firstReplyReceived = true;
            firstReply.countDown();
        } else {
            try {
                WsTelemetryResponse response = mapper.readValue(message, WsTelemetryResponse.class);
                if (!response.getData().isEmpty()) {
                    this.message = response;
                    latch.countDown();
                }
            } catch (IOException e) {
                log.error("ws message can't be read");
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("ws is closed, due to [{}]", reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public WsTelemetryResponse getLastMessage() {
        try {
            boolean result = latch.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (result) {
                return this.message;
            } else {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
        }
        return null;
    }

    void waitForFirstReply() {
        try {
            boolean result = firstReply.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (!result) {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }
}
