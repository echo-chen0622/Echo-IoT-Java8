package org.echoiot.server.common.data;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DynamicProtoUtilsTest {

    @Test
    public void testProtoSchemaWithMessageNestedTypes() throws Exception {
        @NotNull String schema = "syntax = \"proto3\";\n" +
                                 "\n" +
                                 "package testnested;\n" +
                                 "\n" +
                                 "message Outer {\n" +
                                 "  message MiddleAA {\n" +
                                 "    message Inner {\n" +
                                 "      optional int64 ival = 1;\n" +
                                 "      optional bool  booly = 2;\n" +
                                 "    }\n" +
                                 "    Inner inner = 1;\n" +
                                 "  }\n" +
                                 "  message MiddleBB {\n" +
                                 "    message Inner {\n" +
                                 "      optional int32 ival = 1;\n" +
                                 "      optional bool  booly = 2;\n" +
                                 "    }\n" +
                                 "    Inner inner = 1;\n" +
                                 "  }\n" +
                                 "  MiddleAA middleAA = 1;\n" +
                                 "  MiddleBB middleBB = 2;\n" +
                                 "}";
        @NotNull ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(schema);
        DynamicSchema dynamicSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, "test schema with nested types");
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(5, messageTypes.size());
        assertTrue(messageTypes.contains("testnested.Outer"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA.Inner"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB.Inner"));

        DynamicMessage.Builder middleAAInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleAAInnerMsgDescriptor = middleAAInnerMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage middleAAInnerMsg = middleAAInnerMsgBuilder
                .setField(middleAAInnerMsgDescriptor.findFieldByName("ival"), 1L)
                .setField(middleAAInnerMsgDescriptor.findFieldByName("booly"), true)
                .build();

        DynamicMessage.Builder middleAAMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA");
        Descriptors.Descriptor middleAAMsgDescriptor = middleAAMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage middleAAMsg = middleAAMsgBuilder
                .setField(middleAAMsgDescriptor.findFieldByName("inner"), middleAAInnerMsg)
                .build();

        DynamicMessage.Builder middleBBInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleBBInnerMsgDescriptor = middleBBInnerMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage middleBBInnerMsg = middleBBInnerMsgBuilder
                .setField(middleBBInnerMsgDescriptor.findFieldByName("ival"), 0L)
                .setField(middleBBInnerMsgDescriptor.findFieldByName("booly"), false)
                .build();

        DynamicMessage.Builder middleBBMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleBB");
        Descriptors.Descriptor middleBBMsgDescriptor = middleBBMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage middleBBMsg = middleBBMsgBuilder
                .setField(middleBBMsgDescriptor.findFieldByName("inner"), middleBBInnerMsg)
                .build();


        DynamicMessage.Builder outerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer");
        Descriptors.Descriptor outerMsgBuilderDescriptor = outerMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage outerMsg = outerMsgBuilder
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleAA"), middleAAMsg)
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleBB"), middleBBMsg)
                .build();

        assertEquals("{\n" +
                "  \"middleAA\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": \"1\",\n" +
                "      \"booly\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"middleBB\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": 0,\n" +
                "      \"booly\": false\n" +
                "    }\n" +
                "  }\n" +
                "}", DynamicProtoUtils.dynamicMsgToJson(outerMsgBuilderDescriptor, outerMsg.toByteArray()));
    }

    @Test
    public void testProtoSchemaWithMessageOneOfs() throws Exception {
        @NotNull String schema = "syntax = \"proto3\";\n" +
                                 "\n" +
                                 "package testoneofs;\n" +
                                 "\n" +
                                 "message SubMessage {\n" +
                                 "   repeated string name = 1;\n" +
                                 "}\n" +
                                 "\n" +
                                 "message SampleMessage {\n" +
                                 "  optional int32 id = 1;\n" +
                                 "  oneof testOneOf {\n" +
                                 "     string name = 4;\n" +
                                 "     SubMessage subMessage = 9;\n" +
                                 "  }\n" +
                                 "}";
        @NotNull ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(schema);
        DynamicSchema dynamicSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, "test schema with message oneOfs");
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(2, messageTypes.size());
        assertTrue(messageTypes.contains("testoneofs.SubMessage"));
        assertTrue(messageTypes.contains("testoneofs.SampleMessage"));

        DynamicMessage.Builder sampleMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SampleMessage");
        Descriptors.Descriptor sampleMsgDescriptor = sampleMsgBuilder.getDescriptorForType();
        assertNotNull(sampleMsgDescriptor);

        @NotNull List<Descriptors.FieldDescriptor> fields = sampleMsgDescriptor.getFields();
        assertEquals(3, fields.size());
        @NotNull DynamicMessage sampleMsg = sampleMsgBuilder
                .setField(sampleMsgDescriptor.findFieldByName("name"), "Bob")
                .build();
        assertEquals("{\n" + "  \"name\": \"Bob\"\n" + "}", DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsg.toByteArray()));

        DynamicMessage.Builder subMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SubMessage");
        Descriptors.Descriptor subMsgDescriptor = subMsgBuilder.getDescriptorForType();
        @NotNull DynamicMessage subMsg = subMsgBuilder
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "Alice")
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "John")
                .build();

        @NotNull DynamicMessage sampleMsgWithOneOfSubMessage = sampleMsgBuilder.setField(sampleMsgDescriptor.findFieldByName("subMessage"), subMsg).build();
        assertEquals("{\n" + "  \"subMessage\": {\n" + "    \"name\": [\"Alice\", \"John\"]\n" + "  }\n" + "}",
                DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsgWithOneOfSubMessage.toByteArray()));
    }

}
