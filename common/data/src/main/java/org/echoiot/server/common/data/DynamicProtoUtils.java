package org.echoiot.server.common.data;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DynamicProtoUtils {

    public static final Location LOCATION = new Location("", "", -1, -1);
    public static final String PROTO_3_SYNTAX = "proto3";

    @Nullable
    public static Descriptors.Descriptor getDescriptor(@NotNull String protoSchema, @NotNull String schemaName) {
        try {
            DynamicMessage.Builder builder = getDynamicMessageBuilder(protoSchema, schemaName);
            return builder.getDescriptorForType();
        } catch (Exception e) {
            log.warn("Failed to get Message Descriptor due to {}", e.getMessage());
            return null;
        }
    }

    public static DynamicMessage.Builder getDynamicMessageBuilder(@NotNull String protoSchema, @NotNull String schemaName) {
        @NotNull ProtoFileElement protoFileElement = getProtoFileElement(protoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(protoFileElement, schemaName);
        @NotNull String lastMsgName = getMessageTypes(protoFileElement.getTypes()).stream()
                                                                                  .map(MessageElement::getName).reduce((previous, last) -> last).get();
        return dynamicSchema.newMessageBuilder(lastMsgName);
    }

    public static DynamicSchema getDynamicSchema(@NotNull ProtoFileElement protoFileElement, @NotNull String schemaName) {
        @NotNull DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        schemaBuilder.setName(schemaName);
        schemaBuilder.setSyntax(PROTO_3_SYNTAX);
        schemaBuilder.setPackage(StringUtils.isNotEmpty(protoFileElement.getPackageName()) ?
                protoFileElement.getPackageName() : schemaName.toLowerCase());
        @NotNull List<TypeElement> types = protoFileElement.getTypes();
        @NotNull List<MessageElement> messageTypes = getMessageTypes(types);

        if (!messageTypes.isEmpty()) {
            @NotNull List<EnumElement> enumTypes = getEnumElements(types);
            if (!enumTypes.isEmpty()) {
                enumTypes.forEach(enumElement -> {
                    EnumDefinition enumDefinition = getEnumDefinition(enumElement);
                    schemaBuilder.addEnumDefinition(enumDefinition);
                });
            }
            @NotNull List<MessageDefinition> messageDefinitions = getMessageDefinitions(messageTypes);
            messageDefinitions.forEach(schemaBuilder::addMessageDefinition);
            try {
                return schemaBuilder.build();
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException("Failed to create dynamic schema due to: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Failed to get Dynamic Schema! Message types is empty for schema:" + schemaName);
        }
    }

    @NotNull
    public static ProtoFileElement getProtoFileElement(@NotNull String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }

    public static String dynamicMsgToJson(@NotNull Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        @NotNull DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    @NotNull
    public static DynamicMessage jsonToDynamicMessage(@NotNull DynamicMessage.Builder builder, String payload) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(payload, builder);
        return builder.build();
    }

    @NotNull
    private static List<MessageElement> getMessageTypes(@NotNull List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());
    }

    @NotNull
    private static List<EnumElement> getEnumElements(@NotNull List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());
    }

    @NotNull
    private static List<MessageDefinition> getMessageDefinitions(@NotNull List<MessageElement> messageElementsList) {
        if (!messageElementsList.isEmpty()) {
            @NotNull List<MessageDefinition> messageDefinitions = new ArrayList<>();
            messageElementsList.forEach(messageElement -> {
                @NotNull MessageDefinition.Builder messageDefinitionBuilder = MessageDefinition.newBuilder(messageElement.getName());

                @NotNull List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    @NotNull List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        nestedEnumTypes.forEach(enumElement -> {
                            EnumDefinition nestedEnumDefinition = getEnumDefinition(enumElement);
                            messageDefinitionBuilder.addEnumDefinition(nestedEnumDefinition);
                        });
                    }
                    @NotNull List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    @NotNull List<MessageDefinition> nestedMessageDefinitions = getMessageDefinitions(nestedMessageTypes);
                    nestedMessageDefinitions.forEach(messageDefinitionBuilder::addMessageDefinition);
                }
                @NotNull List<FieldElement> messageElementFields = messageElement.getFields();
                @NotNull List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    for (@NotNull OneOfElement oneOfelement : oneOfs) {
                        MessageDefinition.OneofBuilder oneofBuilder = messageDefinitionBuilder.addOneof(oneOfelement.getName());
                        addMessageFieldsToTheOneOfDefinition(oneOfelement.getFields(), oneofBuilder);
                    }
                }
                if (!messageElementFields.isEmpty()) {
                    addMessageFieldsToTheMessageDefinition(messageElementFields, messageDefinitionBuilder);
                }
                messageDefinitions.add(messageDefinitionBuilder.build());
            });
            return messageDefinitions;
        } else {
            return Collections.emptyList();
        }
    }

    private static EnumDefinition getEnumDefinition(@NotNull EnumElement enumElement) {
        @NotNull List<EnumConstantElement> enumElementTypeConstants = enumElement.getConstants();
        @NotNull EnumDefinition.Builder enumDefinitionBuilder = EnumDefinition.newBuilder(enumElement.getName());
        if (!enumElementTypeConstants.isEmpty()) {
            enumElementTypeConstants.forEach(constantElement -> enumDefinitionBuilder.addValue(constantElement.getName(), constantElement.getTag()));
        }
        return enumDefinitionBuilder.build();
    }


    private static void addMessageFieldsToTheMessageDefinition(@NotNull List<FieldElement> messageElementFields, @NotNull MessageDefinition.Builder messageDefinitionBuilder) {
        messageElementFields.forEach(fieldElement -> {
            @Nullable String labelStr = null;
            if (fieldElement.getLabel() != null) {
                labelStr = fieldElement.getLabel().name().toLowerCase();
            }
            messageDefinitionBuilder.addField(
                    labelStr,
                    fieldElement.getType(),
                    fieldElement.getName(),
                    fieldElement.getTag());
        });
    }

    private static void addMessageFieldsToTheOneOfDefinition(@NotNull List<FieldElement> oneOfsElementFields, @NotNull MessageDefinition.OneofBuilder oneofBuilder) {
        oneOfsElementFields.forEach(fieldElement -> oneofBuilder.addField(
                fieldElement.getType(),
                fieldElement.getName(),
                fieldElement.getTag()));
        oneofBuilder.msgDefBuilder();
    }

    // validation

    public static void validateProtoSchema(@NotNull String schema, String schemaName, String exceptionPrefix) throws IllegalArgumentException {
        @NotNull ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException(exceptionPrefix + " failed to parse " + schemaName + " due to: " + e.getMessage());
        }
        checkProtoFileSyntax(schemaName, protoFileElement);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getOptions().isEmpty(), " Schema options don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getPublicImports().isEmpty(), " Schema public imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getImports().isEmpty(), " Schema imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getExtendDeclarations().isEmpty(), " Schema extend declarations don't support!", exceptionPrefix);
        checkTypeElements(schemaName, protoFileElement, exceptionPrefix);
    }

    private static void checkProtoFileSyntax(String schemaName, @NotNull ProtoFileElement protoFileElement) {
        if (protoFileElement.getSyntax() == null || !protoFileElement.getSyntax().equals(Syntax.PROTO_3)) {
            throw new IllegalArgumentException("[Transport Configuration] invalid schema syntax: " + protoFileElement.getSyntax() +
                    " for " + schemaName + " provided! Only " + Syntax.PROTO_3 + " allowed!");
        }
    }

    private static void checkProtoFileCommonSettings(String schemaName, boolean isEmptySettings, String invalidSettingsMessage, String exceptionPrefix) {
        if (!isEmptySettings) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + invalidSettingsMessage);
        }
    }

    private static void checkTypeElements(String schemaName, @NotNull ProtoFileElement protoFileElement, String exceptionPrefix) {
        @NotNull List<TypeElement> types = protoFileElement.getTypes();
        if (!types.isEmpty()) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " At least one Message definition should exists!");
            } else {
                checkEnumElements(schemaName, getEnumElements(types), exceptionPrefix);
                checkMessageElements(schemaName, getMessageTypes(types), exceptionPrefix);
            }
        } else {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Type elements is empty!");
        }
    }

    private static void checkFieldElements(String schemaName, @NotNull List<FieldElement> fieldElements, String exceptionPrefix) {
        if (!fieldElements.isEmpty()) {
            boolean hasRequiredLabel = fieldElements.stream().anyMatch(fieldElement -> {
                @Nullable Field.Label label = fieldElement.getLabel();
                return label != null && label.equals(Field.Label.REQUIRED);
            });
            if (hasRequiredLabel) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Required labels are not supported!");
            }
            boolean hasDefaultValue = fieldElements.stream().anyMatch(fieldElement -> fieldElement.getDefaultValue() != null);
            if (hasDefaultValue) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Default values are not supported!");
            }
        }
    }

    private static void checkEnumElements(String schemaName, @NotNull List<EnumElement> enumTypes, String exceptionPrefix) {
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getNestedTypes().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Nested types in Enum definitions are not supported!");
        }
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getOptions().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Enum definitions options are not supported!");
        }
    }

    private static void checkMessageElements(String schemaName, @NotNull List<MessageElement> messageElementsList, String exceptionPrefix) {
        if (!messageElementsList.isEmpty()) {
            messageElementsList.forEach(messageElement -> {
                checkProtoFileCommonSettings(schemaName, messageElement.getGroups().isEmpty(),
                        " Message definition groups don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getOptions().isEmpty(),
                        " Message definition options don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getExtensions().isEmpty(),
                        " Message definition extensions don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getReserveds().isEmpty(),
                        " Message definition reserved elements don't support!", exceptionPrefix);
                checkFieldElements(schemaName, messageElement.getFields(), exceptionPrefix);
                @NotNull List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    oneOfs.forEach(oneOfElement -> {
                        checkProtoFileCommonSettings(schemaName, oneOfElement.getGroups().isEmpty(),
                                " OneOf definition groups don't support!", exceptionPrefix);
                        checkFieldElements(schemaName, oneOfElement.getFields(), exceptionPrefix);
                    });
                }
                @NotNull List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    @NotNull List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        checkEnumElements(schemaName, nestedEnumTypes, exceptionPrefix);
                    }
                    @NotNull List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    checkMessageElements(schemaName, nestedMessageTypes, exceptionPrefix);
                }
            });
        }
    }

    @NotNull
    public static String invalidSchemaProvidedMessage(String schemaName, String exceptionPrefix) {
        return exceptionPrefix + " invalid " + schemaName + " provided!";
    }

}
