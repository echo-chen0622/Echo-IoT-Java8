package org.echoiot.server.transport.coap;

import org.echoiot.server.common.msg.session.FeatureType;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CoapTestClient {

    private static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    @NotNull
    private final CoapClient client;

    public CoapTestClient(){
        this.client = createClient();
    }

    public CoapTestClient(String accessToken, @NotNull FeatureType featureType) {
        this.client = createClient(getFeatureTokenUrl(accessToken, featureType));
    }

    public CoapTestClient(String featureTokenUrl) {
        this.client = createClient(featureTokenUrl);
    }

    public void connectToCoap(String accessToken) {
        setURI(accessToken, null);
    }

    public void connectToCoap(String accessToken, FeatureType featureType) {
        setURI(accessToken, featureType);
    }

    public void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    public CoapResponse postMethod(@NotNull String requestBody) throws ConnectorException, IOException {
        return this.postMethod(requestBody.getBytes());
    }

    public CoapResponse postMethod(byte[] requestBodyBytes) throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(requestBodyBytes, MediaTypeRegistry.APPLICATION_JSON);
    }

    public void postMethod(CoapHandler handler, String payload, int format) {
        client.post(handler, payload, format);
    }

    public void postMethod(CoapHandler handler, byte[] payload, int format) {
        client.post(handler, payload, format);
    }

    public CoapResponse getMethod() throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
    }

    public CoapObserveRelation getObserveRelation(CoapTestCallback callback){
        @NotNull Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        return client.observe(request, callback);
    }

    public void setURI(String featureTokenUrl) {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        client.setURI(featureTokenUrl);
    }

    public void setURI(String accessToken, @Nullable FeatureType featureType) {
        if (featureType == null){
            featureType = FeatureType.ATTRIBUTES;
        }
        setURI(getFeatureTokenUrl(accessToken, featureType));
    }

    @NotNull
    private CoapClient createClient() {
        return new CoapClient();
    }

    @NotNull
    private CoapClient createClient(String featureTokenUrl) {
        return new CoapClient(featureTokenUrl);
    }

    @NotNull
    public static String getFeatureTokenUrl(String token, @NotNull FeatureType featureType) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    @NotNull
    public static String getFeatureTokenUrl(String token, @NotNull FeatureType featureType, int requestId) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase() + "/" + requestId;
    }
}
