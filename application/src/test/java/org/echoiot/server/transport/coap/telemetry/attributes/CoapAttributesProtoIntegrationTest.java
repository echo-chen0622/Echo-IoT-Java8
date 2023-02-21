package org.echoiot.server.transport.coap.telemetry.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.echoiot.server.common.data.CoapDeviceType;
import org.echoiot.server.common.data.DynamicProtoUtils;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.echoiot.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.echoiot.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.echoiot.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.echoiot.server.dao.service.DaoSqlTest;
import org.echoiot.server.transport.coap.CoapTestConfigProperties;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@DaoSqlTest
public class CoapAttributesProtoIntegrationTest extends CoapAttributesIntegrationTest {

    @Before
    @Override
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attributes device Proto")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testPushAttributes() throws Exception {
        DynamicSchema attributesSchema = getDynamicSchema();
        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        @NotNull DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        @NotNull DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        @NotNull DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("key2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("key3"), 3.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("key4"), 4)
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(Arrays.asList("key1", "key2", "key3", "key4", "key5"), postAttributesMsg.toByteArray(), false);
    }

    @Test
    public void testPushAttributesWithExplicitPresenceProtoKeys() throws Exception {
        DynamicSchema attributesSchema = getDynamicSchema();
        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        @NotNull DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        @NotNull DynamicMessage jsonObject = jsonObjectBuilder
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        @NotNull DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("key1"), "")
                .setField(postAttributesMsgDescriptor.findFieldByName("key5"), jsonObject)
                .build();
        processAttributesTest(Arrays.asList("key1", "key5"), postAttributesMsg.toByteArray(), true);
    }

    private DynamicSchema getDynamicSchema() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        @NotNull CoapDeviceProfileTransportConfiguration coapTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        @NotNull CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        @NotNull DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        @NotNull TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        @NotNull ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        String deviceAttributesProtoSchema = protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema();
        @NotNull ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(deviceAttributesProtoSchema);
        return DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);
    }

}
