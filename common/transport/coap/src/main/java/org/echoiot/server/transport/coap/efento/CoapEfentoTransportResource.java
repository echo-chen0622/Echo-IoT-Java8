package org.echoiot.server.transport.coap.efento;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.auth.SessionInfoCreator;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.coap.MeasurementTypeProtos;
import org.echoiot.server.gen.transport.coap.MeasurementsProtos;
import org.echoiot.server.transport.coap.AbstractCoapTransportResource;
import org.echoiot.server.transport.coap.CoapTransportContext;
import org.echoiot.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.echoiot.server.transport.coap.callback.CoapEfentoCallback;
import org.echoiot.server.transport.coap.efento.utils.CoapEfentoUtils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.echoiot.server.transport.coap.CoapTransportService.*;

@Slf4j
public class CoapEfentoTransportResource extends AbstractCoapTransportResource {

    private static final int CHILD_RESOURCE_POSITION = 2;

    public CoapEfentoTransportResource(@NotNull CoapTransportContext context, String name) {
        super(context, name);
        this.setObservable(true); // enable observing
        this.setObserveType(CoAP.Type.CON); // configure the notification type to CONs
//        this.getAttributes().setObservable(); // mark observable in the Link-Format
    }

    @Override
    protected void processHandleGet(@NotNull CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        boolean validPath = uriPath.size() == CHILD_RESOURCE_POSITION && uriPath.get(1).equals(CURRENT_TIMESTAMP);
        if (!validPath) {
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            int dateInSec = (int) (System.currentTimeMillis() / 1000);
            @NotNull byte[] bytes = ByteBuffer.allocate(4).putInt(dateInSec).array();
            exchange.respond(CoAP.ResponseCode.CONTENT, bytes);
        }
    }

    @Override
    protected void processHandlePost(@NotNull CoapExchange exchange) {
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() != CHILD_RESOURCE_POSITION) {
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
            return;
        }
        String requestType = uriPath.get(1);
        switch (requestType) {
            case MEASUREMENTS:
                processMeasurementsRequest(exchange, request);
                break;
            case DEVICE_INFO:
            case CONFIGURATION:
                //We respond only to confirmed requests in order to reduce battery consumption for Efento devices.
                if (exchange.advanced().getRequest().isConfirmable()) {
                    exchange.respond(new Response(CoAP.ResponseCode.CREATED));
                }
                break;
            default:
                exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                break;
        }
    }

    private void processMeasurementsRequest(@NotNull CoapExchange exchange, @NotNull Request request) {
        byte[] bytes = request.getPayload();
        try {
            MeasurementsProtos.ProtoMeasurements protoMeasurements = MeasurementsProtos.ProtoMeasurements.parseFrom(bytes);
            log.trace("Successfully parsed Efento ProtoMeasurements: [{}]", protoMeasurements.getCloudToken());
            @NotNull String token = protoMeasurements.getCloudToken();
            transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(token).build(),
                                     new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                        TransportProtos.SessionInfoProto sessionInfo = SessionInfoCreator.create(msg, transportContext, UUID.randomUUID());
                        @NotNull UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
                        try {
                            validateEfentoTransportConfiguration(deviceProfile);
                            @NotNull List<EfentoMeasurements> efentoMeasurements = getEfentoMeasurements(protoMeasurements, sessionId);
                            transportService.process(sessionInfo,
                                    transportContext.getEfentoCoapAdaptor().convertToPostTelemetry(sessionId, efentoMeasurements),
                                    new CoapEfentoCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
                            reportSubscriptionInfo(sessionInfo, false, false);
                        } catch (AdaptorException e) {
                            log.error("[{}] Failed to decode Efento ProtoMeasurements: ", sessionId, e);
                            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                        }
                    }));
        } catch (Exception e) {
            log.error("Failed to decode Efento ProtoMeasurements: ", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @NotNull
    @Override
    public Resource getChild(String name) {
        return this;
    }

    private void validateEfentoTransportConfiguration(@NotNull DeviceProfile deviceProfile) throws AdaptorException {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            @NotNull CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration =
                    (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            if (!(coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration() instanceof EfentoCoapDeviceTypeConfiguration)) {
                throw new AdaptorException("Invalid CoapDeviceTypeConfiguration type: " + coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration().getClass().getSimpleName() + "!");
            }
        } else {
            throw new AdaptorException("Invalid DeviceProfileTransportConfiguration type" + transportConfiguration.getClass().getSimpleName() + "!");
        }
    }

    @NotNull
    private List<EfentoMeasurements> getEfentoMeasurements(@NotNull MeasurementsProtos.ProtoMeasurements protoMeasurements, UUID sessionId) {
        @NotNull String serialNumber = CoapEfentoUtils.convertByteArrayToString(protoMeasurements.getSerialNum().toByteArray());
        boolean batteryStatus = protoMeasurements.getBatteryStatus();
        int measurementPeriodBase = protoMeasurements.getMeasurementPeriodBase();
        int measurementPeriodFactor = protoMeasurements.getMeasurementPeriodFactor();
        int signal = protoMeasurements.getSignal();
        @NotNull List<MeasurementsProtos.ProtoChannel> channelsList = protoMeasurements.getChannelsList();
        @NotNull Map<Long, JsonObject> valuesMap = new TreeMap<>();
        if (!CollectionUtils.isEmpty(channelsList)) {
            int channel = 0;
            JsonObject values;
            for (@NotNull MeasurementsProtos.ProtoChannel protoChannel : channelsList) {
                channel++;
                boolean isBinarySensor = false;
                @NotNull MeasurementTypeProtos.MeasurementType measurementType = protoChannel.getType();
                @NotNull String measurementTypeName = measurementType.name();
                if (measurementType.equals(MeasurementTypeProtos.MeasurementType.OK_ALARM)
                        || measurementType.equals(MeasurementTypeProtos.MeasurementType.FLOODING)) {
                    isBinarySensor = true;
                }
                if (measurementPeriodFactor == 0 && isBinarySensor) {
                    measurementPeriodFactor = 14;
                } else {
                    measurementPeriodFactor = 1;
                }
                int measurementPeriod = measurementPeriodBase * measurementPeriodFactor;
                long measurementPeriodMillis = TimeUnit.SECONDS.toMillis(measurementPeriod);
                long nextTransmissionAtMillis = TimeUnit.SECONDS.toMillis(protoMeasurements.getNextTransmissionAt());
                int startPoint = protoChannel.getStartPoint();
                int startTimestamp = protoChannel.getTimestamp();
                long startTimestampMillis = TimeUnit.SECONDS.toMillis(startTimestamp);
                @NotNull List<Integer> sampleOffsetsList = protoChannel.getSampleOffsetsList();
                if (!CollectionUtils.isEmpty(sampleOffsetsList)) {
                    int sampleOfssetsListSize = sampleOffsetsList.size();
                    for (int i = 0; i < sampleOfssetsListSize; i++) {
                        int sampleOffset = sampleOffsetsList.get(i);
                        Integer previousSampleOffset = isBinarySensor && i > 0 ? sampleOffsetsList.get(i - 1) : null;
                        if (sampleOffset == -32768) {
                            log.warn("[{}],[{}] Sensor error value! Ignoring.", sessionId, sampleOffset);
                        } else {
                            switch (measurementType) {
                                case TEMPERATURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("temperature_" + channel, ((double) (startPoint + sampleOffset)) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case WATER_METER:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pulse_counter_water_" + channel, ((double) (startPoint + sampleOffset)));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case HUMIDITY:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("humidity_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case ATMOSPHERIC_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_" + channel, (double) (startPoint + sampleOffset) / 10f);
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case DIFFERENTIAL_PRESSURE:
                                    values = valuesMap.computeIfAbsent(startTimestampMillis, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("pressure_diff_" + channel, (double) (startPoint + sampleOffset));
                                    startTimestampMillis = startTimestampMillis + measurementPeriodMillis;
                                    break;
                                case OK_ALARM:
                                    boolean currentIsOk = sampleOffset < 0;
                                    if (previousSampleOffset != null) {
                                        boolean previousIsOk = previousSampleOffset < 0;
                                        boolean isOk = previousIsOk && currentIsOk;
                                        boolean isAlarm = !previousIsOk && !currentIsOk;
                                        if (isOk || isAlarm) {
                                            break;
                                        }
                                    }
                                    @NotNull String data = currentIsOk ? "OK" : "ALARM";
                                    long sampleOffsetMillis = TimeUnit.SECONDS.toMillis(sampleOffset);
                                    long measurementTimestamp = startTimestampMillis + Math.abs(sampleOffsetMillis);
                                    values = valuesMap.computeIfAbsent(measurementTimestamp - 1000, k ->
                                            CoapEfentoUtils.setDefaultMeasurements(serialNumber, batteryStatus, measurementPeriod, nextTransmissionAtMillis, signal, k));
                                    values.addProperty("ok_alarm_" + channel, data);
                                    break;
                                case NO_SENSOR:
                                case UNRECOGNIZED:
                                    log.trace("[{}][{}] Sensor error value! Ignoring.", sessionId, measurementTypeName);
                                    break;
                                default:
                                    log.trace("[{}],[{}] Unsupported measurementType! Ignoring.", sessionId, measurementTypeName);
                                    break;
                            }
                        }
                    }
                } else {
                    log.trace("[{}][{}] sampleOffsetsList list is empty!", sessionId, measurementTypeName);
                }
            }
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to get Efento measurements, reason: channels list is empty!");
        }
        if (!CollectionUtils.isEmpty(valuesMap)) {
            @NotNull List<EfentoMeasurements> efentoMeasurements = new ArrayList<>();
            for (Long ts : valuesMap.keySet()) {
                @NotNull EfentoMeasurements measurement = new EfentoMeasurements(ts, valuesMap.get(ts));
                efentoMeasurements.add(measurement);
            }
            return efentoMeasurements;
        } else {
            throw new IllegalStateException("[" + sessionId + "]: Failed to collect Efento measurements, reason, values map is empty!");
        }
    }

    @Data
    @AllArgsConstructor
    public static class EfentoMeasurements {

        private long ts;
        private JsonObject values;

    }
}
