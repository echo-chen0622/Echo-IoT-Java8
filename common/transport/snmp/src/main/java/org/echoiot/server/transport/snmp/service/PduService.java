package org.echoiot.server.transport.snmp.service;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.echoiot.server.common.data.kv.DataType;
import org.echoiot.server.common.data.transport.snmp.SnmpMapping;
import org.echoiot.server.common.data.transport.snmp.SnmpMethod;
import org.echoiot.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.echoiot.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.echoiot.server.queue.util.TbSnmpTransportComponent;
import org.echoiot.server.transport.snmp.session.DeviceSessionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.smi.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TbSnmpTransportComponent
@Service
@Slf4j
public class PduService {
    @NotNull
    public PDU createPdu(@NotNull DeviceSessionContext sessionContext, @NotNull SnmpCommunicationConfig communicationConfig, @NotNull Map<String, String> values) {
        @NotNull PDU pdu = setUpPdu(sessionContext);

        pdu.setType(communicationConfig.getMethod().getCode());
        pdu.addAll(communicationConfig.getAllMappings().stream()
                .filter(mapping -> values.isEmpty() || values.containsKey(mapping.getKey()))
                .map(mapping -> Optional.ofNullable(values.get(mapping.getKey()))
                        .map(value -> {
                            @NotNull Variable variable = toSnmpVariable(value, mapping.getDataType());
                            return new VariableBinding(new OID(mapping.getOid()), variable);
                        })
                        .orElseGet(() -> new VariableBinding(new OID(mapping.getOid()))))
                .collect(Collectors.toList()));

        return pdu;
    }

    @NotNull
    public PDU createSingleVariablePdu(@NotNull DeviceSessionContext sessionContext, @NotNull SnmpMethod snmpMethod, String oid, @Nullable String value, DataType dataType) {
        @NotNull PDU pdu = setUpPdu(sessionContext);
        pdu.setType(snmpMethod.getCode());

        Variable variable = value == null ? Null.instance : toSnmpVariable(value, dataType);
        pdu.add(new VariableBinding(new OID(oid), variable));

        return pdu;
    }

    @NotNull
    private Variable toSnmpVariable(@NotNull String value, DataType dataType) {
        dataType = dataType == null ? DataType.STRING : dataType;
        Variable variable;
        switch (dataType) {
            case LONG:
                try {
                    variable = new Integer32(Integer.parseInt(value));
                    break;
                } catch (NumberFormatException ignored) {
                }
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case JSON:
            default:
                variable = new OctetString(value);
        }
        return variable;
    }

    @NotNull
    private PDU setUpPdu(@NotNull DeviceSessionContext sessionContext) {
        PDU pdu;
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        SnmpProtocolVersion snmpVersion = deviceTransportConfiguration.getProtocolVersion();
        switch (snmpVersion) {
            case V1:
            case V2C:
                pdu = new PDU();
                break;
            case V3:
                @NotNull ScopedPDU scopedPdu = new ScopedPDU();
                scopedPdu.setContextName(new OctetString(deviceTransportConfiguration.getContextName()));
                scopedPdu.setContextEngineID(new OctetString(deviceTransportConfiguration.getEngineId()));
                pdu = scopedPdu;
                break;
            default:
                throw new UnsupportedOperationException("SNMP version " + snmpVersion + " is not supported");
        }
        return pdu;
    }


    @NotNull
    public JsonObject processPdu(@NotNull PDU pdu, @Nullable List<SnmpMapping> responseMappings) {
        @NotNull Map<OID, String> values = processPdu(pdu);

        @NotNull Map<OID, SnmpMapping> mappings = new HashMap<>();
        if (responseMappings != null) {
            for (@NotNull SnmpMapping mapping : responseMappings) {
                @NotNull OID oid = new OID(mapping.getOid());
                mappings.put(oid, mapping);
            }
        }

        @NotNull JsonObject data = new JsonObject();
        values.forEach((oid, value) -> {
            log.trace("Processing variable binding: {} - {}", oid, value);

            SnmpMapping mapping = mappings.get(oid);
            if (mapping == null) {
                log.debug("No SNMP mapping for oid {}", oid);
                return;
            }

            processValue(mapping.getKey(), mapping.getDataType(), value, data);
        });

        return data;
    }

    @NotNull
    public Map<OID, String> processPdu(@NotNull PDU pdu) {
        return IntStream.range(0, pdu.size())
                .mapToObj(pdu::get)
                .filter(Objects::nonNull)
                .filter(variableBinding -> !(variableBinding.getVariable() instanceof Null))
                .collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::toValueString));
    }

    public void processValue(@NotNull String key, @NotNull DataType dataType, @NotNull String value, @NotNull JsonObject result) {
        switch (dataType) {
            case LONG:
                result.addProperty(key, Long.parseLong(value));
                break;
            case BOOLEAN:
                result.addProperty(key, Boolean.parseBoolean(value));
                break;
            case DOUBLE:
                result.addProperty(key, Double.parseDouble(value));
                break;
            case STRING:
            case JSON:
            default:
                result.addProperty(key, value);
        }
    }
}
