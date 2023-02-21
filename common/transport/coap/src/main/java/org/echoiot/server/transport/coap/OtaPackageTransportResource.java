package org.echoiot.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.security.DeviceTokenCredentials;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.common.transport.auth.SessionInfoCreator;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class OtaPackageTransportResource extends AbstractCoapTransportResource {
    private static final int ACCESS_TOKEN_POSITION = 2;

    @NotNull
    private final OtaPackageType otaPackageType;

    public OtaPackageTransportResource(@NotNull CoapTransportContext ctx, @NotNull OtaPackageType otaPackageType) {
        super(ctx, otaPackageType.getKeyPrefix());
        this.otaPackageType = otaPackageType;

        this.setObservable(true);
    }

    @Override
    protected void processHandleGet(@NotNull CoapExchange exchange) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        exchange.accept();
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        processAccessTokenRequest(exchange, request);
    }

    @Override
    protected void processHandlePost(@NotNull CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    private void processAccessTokenRequest(@NotNull CoapExchange exchange, @NotNull Request request) {
        @NotNull Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                                 new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                    getOtaPackageCallback(msg, exchange, otaPackageType);
                }));
    }

    private void getOtaPackageCallback(@NotNull ValidateDeviceCredentialsResponse msg, CoapExchange exchange, @NotNull OtaPackageType firmwareType) {
        TenantId tenantId = msg.getDeviceInfo().getTenantId();
        DeviceId deviceId = msg.getDeviceInfo().getDeviceId();
        TransportProtos.GetOtaPackageRequestMsg requestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setType(firmwareType.name()).build();
        transportContext.getTransportService().process(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()), requestMsg, new OtaPackageCallback(exchange));
    }

    @NotNull
    private Optional<DeviceTokenCredentials> decodeCredentials(@NotNull Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() == ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public Resource getChild(String name) {
        return this;
    }

    private class OtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        private final CoapExchange exchange;

        OtaPackageCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(@NotNull TransportProtos.GetOtaPackageResponseMsg msg) {
            String title = exchange.getQueryParameter("title");
            String version = exchange.getQueryParameter("version");
            if (msg.getResponseStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                String firmwareId = new UUID(msg.getOtaPackageIdMSB(), msg.getOtaPackageIdLSB()).toString();
                if ((title == null || msg.getTitle().equals(title)) && (version == null || msg.getVersion().equals(version))) {
                    String strChunkSize = exchange.getQueryParameter("size");
                    String strChunk = exchange.getQueryParameter("chunk");
                    int chunkSize = StringUtils.isEmpty(strChunkSize) ? 0 : Integer.parseInt(strChunkSize);
                    int chunk = StringUtils.isEmpty(strChunk) ? 0 : Integer.parseInt(strChunk);
                    respondOtaPackage(exchange, transportContext.getOtaPackageDataCache().get(firmwareId, chunkSize, chunk));
                } else {
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                }
            } else {
                exchange.respond(CoAP.ResponseCode.NOT_FOUND);
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void respondOtaPackage(@NotNull CoapExchange exchange, @Nullable byte[] data) {
        @NotNull Response response = new Response(CoAP.ResponseCode.CONTENT);
        if (data != null && data.length > 0) {
            response.setPayload(data);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean lastFlag = data.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, 0);
            }
            transportContext.getExecutor().submit(() -> exchange.respond(response));
        }
    }

}
