package org.echoiot.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class NoSecObserveClient {

    private static final long INFINIT_EXCHANGE_LIFETIME = 0L;

    private final CoapClient coapClient;
    private CoapObserveRelation observeRelation;
    private final ExecutorService executor = Executors.newFixedThreadPool(1, EchoiotThreadFactory.forName(getClass().getSimpleName()));
    private final CountDownLatch latch;

    public NoSecObserveClient(String host, int port, String accessToken) throws URISyntaxException {
        @NotNull URI uri = new URI(getFutureUrl(host, port, accessToken));
        this.coapClient = new CoapClient(uri);
        coapClient.setTimeout(INFINIT_EXCHANGE_LIFETIME);
        this.latch = new CountDownLatch(5);
    }

    public void start() {
        executor.submit(() -> {
            try {
                @NotNull Request request = Request.newGet();
                request.setObserve();
                observeRelation = coapClient.observe(request, new CoapHandler() {
                    @Override
                    public void onLoad(@NotNull CoapResponse response) {
                        String responseText = response.getResponseText();
                        CoAP.ResponseCode code = response.getCode();
                        Integer observe = response.getOptions().getObserve();
                        log.info("CoAP Response received! " +
                                        "responseText: {}, " +
                                        "code: {}, " +
                                        "observe seq number: {}",
                                responseText,
                                code,
                                observe);
                        latch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.error("Ack error!");
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred while sending COAP requests: ");
            }
        });
        try {
            latch.await();
            observeRelation.proactiveCancel();
        } catch (InterruptedException e) {
            log.error("Error occurred: ", e);
        }
    }

    @NotNull
    private String getFutureUrl(String host, Integer port, String accessToken) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes";
    }

    public static void main(@NotNull String[] args) throws URISyntaxException {
        log.info("Usage: java -cp ... client.coap.transport.org.echoiot.server.NoSecObserveClient " +
                "host port accessToken");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];

        @NotNull final NoSecObserveClient client = new NoSecObserveClient(host, port, accessToken);
        client.start();
    }
}
