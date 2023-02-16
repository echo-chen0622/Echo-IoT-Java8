package org.echoiot.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.device.data.DeviceData;
import org.echoiot.server.common.data.device.data.DeviceTransportConfiguration;
import org.echoiot.server.common.transport.TransportService;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbSnmpTransportComponent;

import java.util.UUID;

@TbSnmpTransportComponent
@Service
@RequiredArgsConstructor
public class ProtoTransportEntityService {
    private final TransportService transportService;
    private final DataDecodingEncodingService dataDecodingEncodingService;

    public Device getDeviceById(DeviceId id) {
        TransportProtos.GetDeviceResponseMsg deviceProto = transportService.getDevice(TransportProtos.GetDeviceRequestMsg.newBuilder()
                .setDeviceIdMSB(id.getId().getMostSignificantBits())
                .setDeviceIdLSB(id.getId().getLeastSignificantBits())
                .build());

        if (deviceProto == null) {
            return null;
        }

        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(
                deviceProto.getDeviceProfileIdMSB(), deviceProto.getDeviceProfileIdLSB())
        );

        Device device = new Device();
        device.setId(id);
        device.setDeviceProfileId(deviceProfileId);

        DeviceTransportConfiguration deviceTransportConfiguration = (DeviceTransportConfiguration) dataDecodingEncodingService.decode(
                deviceProto.getDeviceTransportConfiguration().toByteArray()
                                                                                                                                     ).orElseThrow(() -> new IllegalStateException("Can't find device transport configuration"));

        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(deviceTransportConfiguration);
        device.setDeviceData(deviceData);

        return device;
    }

    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        TransportProtos.GetDeviceCredentialsResponseMsg deviceCredentialsResponse = transportService.getDeviceCredentials(
                TransportProtos.GetDeviceCredentialsRequestMsg.newBuilder()
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .build()
        );

        return (DeviceCredentials) dataDecodingEncodingService.decode(deviceCredentialsResponse.getDeviceCredentialsData().toByteArray())
                .orElseThrow(() -> new IllegalArgumentException("Device credentials not found"));
    }

    public TransportProtos.GetSnmpDevicesResponseMsg getSnmpDevicesIds(int page, int pageSize) {
        TransportProtos.GetSnmpDevicesRequestMsg requestMsg = TransportProtos.GetSnmpDevicesRequestMsg.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build();
        return transportService.getSnmpDevicesIds(requestMsg);
    }
}